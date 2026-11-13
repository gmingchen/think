"""已收敛到 mini-program-server/deploy/deploy.py，本文件只做兼容转调。

参数原样透传，例如：
    python deploy.py --build     ->  deploy/deploy.py think --build
    python deploy.py --rollback  ->  deploy/deploy.py think --rollback
"""

import subprocess
import sys
from pathlib import Path

PROJECT = 'think'
DEFAULT_ARGS = []


def main():
    for parent in Path(__file__).resolve().parents:
        entry = parent / 'mini-program-server' / 'deploy' / 'deploy.py'
        if entry.exists():
            cmd = [sys.executable, str(entry), PROJECT, *DEFAULT_ARGS, *sys.argv[1:]]
            return subprocess.call(cmd)
    print('未找到统一发布入口 mini-program-server/deploy/deploy.py，请确认目录结构')
    return 1


if __name__ == '__main__':
    sys.exit(main())
