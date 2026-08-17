$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)) {
    throw '请先设置 DASHSCOPE_API_KEY 环境变量。示例见 .env.example。'
}

if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_MODEL)) {
    $env:DASHSCOPE_MODEL = 'qwen3.7-flash'
}
if ([string]::IsNullOrWhiteSpace($env:DASHSCOPE_EMBEDDING_MODEL)) {
    $env:DASHSCOPE_EMBEDDING_MODEL = 'qwen3.7-text-embedding'
}
if ([string]::IsNullOrWhiteSpace($env:RAG_ENABLED)) {
    $env:RAG_ENABLED = 'true'
}

Write-Host "对话模型: $($env:DASHSCOPE_MODEL)" -ForegroundColor Green
Write-Host "向量模型: $($env:DASHSCOPE_EMBEDDING_MODEL)" -ForegroundColor Green
mvn spring-boot:run
