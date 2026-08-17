@echo off
chcp 65001 >nul
cd /d %~dp0

if "%DASHSCOPE_API_KEY%"=="" (
  echo 请先设置 DASHSCOPE_API_KEY 环境变量。示例见 .env.example。
  exit /b 1
)

if "%DASHSCOPE_MODEL%"=="" set DASHSCOPE_MODEL=qwen3.7-flash
if "%DASHSCOPE_EMBEDDING_MODEL%"=="" set DASHSCOPE_EMBEDDING_MODEL=qwen3.7-text-embedding
if "%RAG_ENABLED%"=="" set RAG_ENABLED=true

call mvn spring-boot:run
