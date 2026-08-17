@echo off
chcp 65001 >nul
echo ============================================
echo Cloudflare Tunnel 启动脚本
echo ============================================
echo.

REM 检查 cloudflared 是否已安装
where cloudflared >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 cloudflared，请先安装！
    echo.
    echo 下载地址: https://github.com/cloudflare/cloudflared/releases/latest
    echo 请下载 cloudflared-windows-amd64.exe 并放置到系统 PATH 中
    echo 或下载 MSI 安装包进行安装
    echo.
    pause
    exit /b 1
)

echo [信息] 检测到 cloudflared，正在启动 Tunnel...
echo.

REM 检查配置文件
if not exist ".cloudflared\config.yml" (
    echo [错误] 未找到配置文件 .cloudflared\config.yml
    echo.
    pause
    exit /b 1
)

echo [信息] 使用配置文件: .cloudflared\config.yml
echo [信息] 正在启动 Tunnel 服务...
echo.

REM 运行 cloudflared
cloudflared tunnel --config .cloudflared\config.yml run

pause
