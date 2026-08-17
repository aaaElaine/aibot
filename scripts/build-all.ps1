$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $projectRoot 'chat-bot')
try {
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw '后端打包失败。' }
} finally {
    Pop-Location
}

foreach ($frontend in @('chat-bot-admin', 'chat-bot-h5')) {
    Push-Location (Join-Path $projectRoot $frontend)
    try {
        npm ci
        if ($LASTEXITCODE -ne 0) { throw "$frontend 依赖安装失败。" }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "$frontend 构建失败。" }
    } finally {
        Pop-Location
    }
}

Write-Host '后端、管理后台和 H5 均已打包完成。' -ForegroundColor Green
