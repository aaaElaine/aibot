# 🚀 快速启动指南

当前稳定版本：`v1.0.0`

项目地址：https://github.com/aaaElaine/aibot

下载指定版本：进入 GitHub 的 **Releases** 或 **Tags** 页面，选择版本后下载 Source code 压缩包。

## 本地开发启动

### 前提条件
- Java 17+
- Node.js 18+
- Maven 3.9+

### 1. 启动后端服务

```bash
cd chat-bot

# Windows PowerShell
$env:JAVA_HOME = "D:\software\JDK"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DASHSCOPE_API_KEY = "你的API密钥"
$env:DASHSCOPE_MODEL = "qwen3.7-flash"
$env:DASHSCOPE_EMBEDDING_MODEL = "qwen3.7-text-embedding"
mvn spring-boot:run

# 或使用启动脚本
.\start.ps1
```

后端启动后访问：http://localhost:8081

### 2. 启动管理后台（新终端窗口）

```bash
cd chat-bot-admin
npm install
npm run dev
```

管理后台地址：http://localhost:5174

### 3. 启动 H5 移动端（新终端窗口）

```bash
cd chat-bot-h5
npm install
npm run dev
```

H5 页面地址：http://localhost:5173

---

## Cloudflare Tunnel 配置

### 1. 安装 cloudflared

下载地址：https://github.com/cloudflare/cloudflared/releases/latest

选择 `cloudflared-windows-amd64.exe` 下载，放置到系统 PATH 中。

### 2. 登录并配置

```bash
# 登录 Cloudflare
cloudflared tunnel login

# 创建隧道
cloudflared tunnel create aibot-tunnel

# 配置 DNS
cloudflared tunnel route dns aibot-tunnel h5.example.com
cloudflared tunnel route dns aibot-tunnel admin.example.com
```

### 3. 启动隧道

```bash
# 方式一：双击启动脚本
start-tunnel.bat

# 方式二：命令行启动
cloudflared tunnel --config .cloudflared/config.yml run
```

---

## 项目结构

```
tiny/
├── chat-bot/           # 后端服务 (Spring Boot, 端口 8081)
├── chat-bot-admin/     # 管理后台 (Vue 3, 端口 5174)
├── chat-bot-h5/        # H5 移动端 (Vue 3, 端口 5173)
├── .cloudflared/       # Cloudflare Tunnel 配置
└── Git使用教程.md       # Git 详细教程
```

## 详细文档

- [Git 使用教程](Git使用教程.md)
- [版本更新记录](CHANGELOG.md)
- [Cloudflare Tunnel 文档](https://developers.cloudflare.com)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

## 构建与版本回退

一键构建全部项目：

```powershell
.\scripts\build-all.ps1
```

每次发布遵循语义化版本，并创建不可变的 Git 标签，例如 `v1.0.1`。回退到任意稳定版本：

```powershell
.\scripts\rollback.ps1 -Version v1.0.0
```

回退命令会拒绝覆盖未提交修改，并以 detached HEAD 方式切换，不会改写分支历史。恢复最新版：

```powershell
git switch main
```
