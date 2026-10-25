"""
厨菜记后端部署脚本
先上传到临时路径再切换重启（停机窗口只包含换文件与启动），
发布前校验健康检查基线，新版本冒烟不通过时自动回滚到 .bak。
"""
import paramiko
import os
import time

# 服务器配置
HOST = '47.116.207.3'
PORT = 22
USER = 'root'
PASSWORD = 'Gmc950830.'

# 部署配置
LOCAL_JAR = r'e:\github\think\think-bk\target\think-bk-2.0.0.jar'
REMOTE_DIR = '/opt/think'
REMOTE_JAR = f'{REMOTE_DIR}/think-bk-2.0.0.jar'
REMOTE_JAR_BACKUP = f'{REMOTE_DIR}/think-bk-2.0.0.jar.bak'
REMOTE_JAR_UPLOADING = f'{REMOTE_DIR}/think-bk-2.0.0.jar.uploading'
APP_NAME = 'think-bk'
JAVA_OPTS = '-Xms256m -Xmx512m'
SPRING_OPTS = '--spring.profiles.active=prod'

# 健康检查：直连本机应用端口，绕过 Nginx
HEALTH_URL = 'http://127.0.0.1:8820/think/user/info'
HEALTH_expect_http = '200'          # 应用活着时固定返回 HTTP 200，业务码在 body 里
START_TRIES = 24                   # 最长等待 24 × 5s = 120s
START_INTERVAL = 5
ROLLBACK_TRIES = 12                # 回滚后最长等待 60s

def log(msg):
    print(f'[DEPLOY] {msg}')

def exec_ssh(ssh, cmd, show_output=True):
    """执行远程命令"""
    log(f'> {cmd}')
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=30)
    out = stdout.read().decode('utf-8').strip()
    err = stderr.read().decode('utf-8').strip()
    if show_output:
        if out: print(f'  stdout: {out}')
        if err: print(f'  stderr: {err}')
    return out, err

def stop_app(ssh):
    log('停止应用进程...')
    exec_ssh(ssh, f"pkill -f '{APP_NAME}' || true")
    time.sleep(2)
    out, _ = exec_ssh(ssh, f"ps aux | grep '{APP_NAME}' | grep -v grep", show_output=False)
    if out:
        log('进程仍在运行，强制杀死...')
        exec_ssh(ssh, f"pkill -9 -f '{APP_NAME}' || true")
        time.sleep(2)


def start_app(ssh):
    exec_ssh(ssh, f'nohup java {JAVA_OPTS} -jar {REMOTE_JAR} {SPRING_OPTS} > {REMOTE_DIR}/nohup.out 2>&1 &')


def http_code(ssh):
    """向本机应用端口要一个 HTTP 状态码，拿不到就返回空串"""
    cmd = "curl -s -o /dev/null -w '%{http_code}' --max-time 5 " + HEALTH_URL
    out, _ = exec_ssh(ssh, cmd, show_output=False)
    return out.strip()


def wait_healthy(ssh, tries):
    for i in range(tries):
        code = http_code(ssh)
        if code == HEALTH_expect_http:
            log(f'健康检查通过 (HTTP {code})，第 {i + 1} 次探测')
            return True
        log(f'  等待应用就绪 {i + 1}/{tries}，当前 HTTP: {code or "无响应"}')
        time.sleep(START_INTERVAL)
    return False


def rollback(ssh):
    """用 .bak 覆盖线上 JAR 并重启；失败的包留在 .failed 供排查"""
    log('!!! 新版本冒烟未通过，自动回滚到 .bak !!!')
    exec_ssh(ssh, f'tail -30 {REMOTE_DIR}/nohup.out')
    exec_ssh(ssh, f'cp {REMOTE_JAR} {REMOTE_JAR}.failed || true')
    stop_app(ssh)
    out, _ = exec_ssh(ssh, f'test -f {REMOTE_JAR_BACKUP} && echo yes || echo no', show_output=False)
    if 'yes' not in out:
        log('ERROR: 没有可用的 .bak，无法自动回滚，请人工介入')
        return False
    exec_ssh(ssh, f'cp {REMOTE_JAR_BACKUP} {REMOTE_JAR}')
    log('启动旧版本...')
    start_app(ssh)
    if wait_healthy(ssh, ROLLBACK_TRIES):
        log('回滚成功，旧版本已恢复服务（坏包保留在 think-bk-2.0.0.jar.failed）')
        return True
    log('ERROR: 回滚后仍不健康，请人工介入！')
    return False


def main():
    # 1. 检查本地 JAR 文件
    if not os.path.exists(LOCAL_JAR):
        log(f'ERROR: JAR 文件不存在: {LOCAL_JAR}')
        return
    jar_size = os.path.getsize(LOCAL_JAR) / (1024 * 1024)
    built_at = time.strftime('%m-%d %H:%M', time.localtime(os.path.getmtime(LOCAL_JAR)))
    log(f'JAR 文件大小: {jar_size:.1f} MB，本地构建时间: {built_at}')

    # 2. 连接服务器
    log(f'连接服务器 {HOST}...')
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, PORT, USER, PASSWORD, timeout=15)
    log('连接成功!')

    # 3. 确保远程目录存在
    exec_ssh(ssh, f'mkdir -p {REMOTE_DIR}')

    # 4. 前置条件不满足时，在停止任何服务之前就中止
    has_curl, _ = exec_ssh(ssh, 'command -v curl || true', show_output=False)
    if 'curl' not in has_curl:
        log('ERROR: 服务器上没有 curl，无法做发布后健康检查。已中止，未停止服务。')
        ssh.close()
        return
    baseline = http_code(ssh)
    log(f'发布前基线 HTTP: {baseline or "无响应"}')
    if baseline != HEALTH_expect_http:
        log('ERROR: 基线健康检查就没通过，请先人工确认服务状态。已中止，未停止服务。')
        ssh.close()
        return

    # 5. 上传到临时路径 —— 这一步旧版本仍在正常服务
    log('上传 JAR 到临时路径（服务不中断）...')
    sftp = ssh.open_sftp()
    start_time = time.time()
    sftp.put(LOCAL_JAR, REMOTE_JAR_UPLOADING)
    elapsed = time.time() - start_time
    log(f'上传完成! 耗时 {elapsed:.1f}s, 速度 {jar_size / elapsed:.1f} MB/s')
    sftp.close()

    # 6. 备份线上 JAR，然后 停 → 换 → 启（停机窗口只有这几秒到几十秒）
    exec_ssh(ssh, f'test -f {REMOTE_JAR} && cp {REMOTE_JAR} {REMOTE_JAR_BACKUP} || true')
    stop_app(ssh)
    exec_ssh(ssh, f'mv {REMOTE_JAR_UPLOADING} {REMOTE_JAR}')
    log('启动新版本...')
    start_app(ssh)

    # 7. 冒烟检查
    if wait_healthy(ssh, START_TRIES):
        out, _ = exec_ssh(ssh, f"ps aux | grep '{APP_NAME}' | grep -v grep")
        log('应用进程已运行!' if out else 'WARNING: 健康检查通过但没抓到进程，请检查日志')
        exec_ssh(ssh, 'ss -tlnp | grep 8820')
        log('最近日志:')
        exec_ssh(ssh, f'tail -20 {REMOTE_DIR}/nohup.out')
        ssh.close()
        log('部署完成!')
        return

    # 8. 冒烟失败 → 回滚
    rollback(ssh)
    ssh.close()

if __name__ == '__main__':
    main()
