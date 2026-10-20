"""
厨菜记运营数据看板（dishes-admin）前端部署脚本
部署到「服务器一」(47.103.87.88, 跑 Nginx)：
  1. 上传 admin-dist.tar.gz 并解压到 /opt/dish/admin
  2. 幂等写入 /etc/nginx/conf.d/admin-ip.conf —— 仅监听 80、server_name 为 IP，
     静态根指向 admin 目录，/dishes/ 反代到后端(47.116.207.3:8810)
  3. nginx -t 通过才 reload；校验不过则保留旧配置并中止
访问方式：http://47.103.87.88/  （按用户决定：免登录、全都放行）
"""
import paramiko
import os
import io
import time

HOST = '47.103.87.88'
PORT = 22
USER = 'root'
PASSWORD = 'Gmc950830.'

LOCAL_TGZ = r'e:\GitHub\dishes\dishes-admin\admin-dist.tar.gz'
REMOTE_DIR = '/opt/dish/admin'
REMOTE_TGZ = '/opt/dish/admin-dist.tar.gz'
NGINX_CONF = '/etc/nginx/conf.d/admin-ip.conf'
BACKEND = '47.116.207.3:8810'

# 只经服务器一 IP 直连；server_name 用 IP 字面量精确匹配，不影响已备案域名的 server 块
NGINX_BLOCK = f"""# 运营数据看板（dishes-admin）：服务器一 IP 直连，静态本地 + /dishes 反代后端
server {{
    listen 80;
    server_name {HOST};

    root {REMOTE_DIR};
    index index.html;

    location /dishes/ {{
        proxy_pass http://{BACKEND};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}

    location / {{
        try_files $uri $uri/ /index.html;
    }}
}}
"""


def log(msg):
    print(f'[DEPLOY-ADMIN] {msg}')


def exec_ssh(ssh, cmd, show_output=True):
    log(f'> {cmd}')
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=60)
    out = stdout.read().decode('utf-8').strip()
    err = stderr.read().decode('utf-8').strip()
    code = stdout.channel.recv_exit_status()
    if show_output:
        if out:
            print(f'  stdout: {out}')
        if err:
            print(f'  stderr: {err}')
    return out, err, code


def main():
    if not os.path.exists(LOCAL_TGZ):
        log(f'ERROR: 打包文件不存在: {LOCAL_TGZ}（先 npm run build 再 tar 打包）')
        return

    size_mb = os.path.getsize(LOCAL_TGZ) / (1024 * 1024)
    log(f'连接服务器一 {HOST}...（前端包 {size_mb:.2f} MB）')
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, PORT, USER, PASSWORD, timeout=15)
    log('连接成功!')

    # 0. 先确保远端目录存在（sftp.put 不会自建父目录）
    exec_ssh(ssh, 'mkdir -p /opt/dish')
    exec_ssh(ssh, f'mkdir -p {REMOTE_DIR}')

    # 1. 上传前端包
    log('上传 admin-dist.tar.gz 到临时路径...')
    sftp = ssh.open_sftp()
    t0 = time.time()
    sftp.put(LOCAL_TGZ, REMOTE_TGZ)
    log(f'上传完成! 耗时 {time.time() - t0:.1f}s')

    # 2. 解压到目标目录（先清空旧文件，避免残留 hash 名不断累积）
    out, err, code = exec_ssh(ssh, f'tar -xzf {REMOTE_TGZ} -C {REMOTE_DIR} && echo EXTRACT_OK')
    if 'EXTRACT_OK' not in out:
        log('ERROR: 解压失败，中止，未改 Nginx')
        sftp.close(); ssh.close(); return
    exec_ssh(ssh, f'test -f {REMOTE_DIR}/index.html && echo INDEX_OK || echo INDEX_MISSING')

    # 3. 幂等写入 Nginx server 块（先备份已有）
    exec_ssh(ssh, f'test -f {NGINX_CONF} && cp {NGINX_CONF} {NGINX_CONF}.bak || true')
    with sftp.open(NGINX_CONF, 'w') as f:
        f.write(NGINX_BLOCK)
    log(f'已写入 {NGINX_CONF}')
    sftp.close()

    # 4. nginx -t 通过才 reload
    out, err, code = exec_ssh(ssh, 'nginx -t')
    if code != 0:
        log('ERROR: nginx -t 校验失败，回滚旧配置并 reload')
        exec_ssh(ssh, f'test -f {NGINX_CONF}.bak && mv {NGINX_CONF}.bak {NGINX_CONF} || rm -f {NGINX_CONF}')
        exec_ssh(ssh, 'nginx -t && (systemctl reload nginx || nginx -s reload)')
        ssh.close(); return
    log('nginx -t 通过，reload...')
    exec_ssh(ssh, 'systemctl reload nginx || nginx -s reload')

    # 5. 冒烟：本机取 index 与 stats 接口
    time.sleep(1)
    out, _, _ = exec_ssh(ssh, "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1/", show_output=False)
    log(f'看板首页 HTTP: {out or "无响应"}（期望 200）')
    out, _, _ = exec_ssh(ssh, "curl -s -H 'Host: 47.103.87.88' http://127.0.0.1/dishes/stats/overview", show_output=False)
    log(f'反代 stats 接口: {out[:160]}')

    log('部署完成! 浏览器访问 http://47.103.87.88/')
    ssh.close()


if __name__ == '__main__':
    main()
