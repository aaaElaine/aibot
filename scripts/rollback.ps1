param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v\d+\.\d+\.\d+$')]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not [string]::IsNullOrWhiteSpace((git status --porcelain))) {
    throw '当前目录存在未提交修改，请先提交或暂存后再回退。'
}

git fetch origin --tags
git rev-parse --verify "refs/tags/$Version" 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "版本 $Version 不存在。可用版本：`n$(git tag --list 'v*' --sort=-version:refname)"
}

git switch --detach $Version
Write-Host "已切换到 $Version。恢复最新版本请运行：git switch main" -ForegroundColor Green
