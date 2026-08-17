# 苏福万家智能客服 - 线上部署完整教程

> 本文档将手把手教你如何将苏福万家智能客服系统部署到公网服务器，让所有人都可以通过互联网访问。

---

## 目录

1. [整体架构](#1-整体架构)
2. [方案选型](#2-方案选型)
3. [第一步：购买云服务器](#3-第一步购买云服务器)
4. [第二步：购买域名和备案](#4-第二步购买域名和备案)
5. [第三步：服务器环境搭建](#5-第三步服务器环境搭建)
6. [第四步：配置项目文件](#6-第四步配置项目文件)
7. [第五步：部署服务](#7-第五步部署服务)
8. [第六步：配置Nginx反向代理和HTTPS](#8-第六步配置nginx反向代理和https)
9. [第七步：验证部署](#9-第七步验证部署)
10. [第八步：常用运维命令](#10-第八步常用运维命令)
11. [常见问题排查](#11-常见问题排查)

---

## 1. 整体架构

```
用户浏览器 ──→ Nginx(80/443) ──→ Docker容器
                                      ├── H5前端 (port 3000)   ← 用户访问
                                      ├── 管理后台 (port 3001)  ← 管理员访问
                                      └── 后端API (port 8080)   ← 被前端代理调用
                                            └── PostgreSQL (port 5432)
                                                  └── 阿里云DashScope AI
```

**域名规划（示例）：**
- `chat.yourdomain.com` → H5 用户端
- `admin.yourdomain.com` → 管理后台
- `api.yourdomain.com` → 后端 API（可选，用于调试）

---

## 2. 方案选型

### 推荐方案：阿里云 ECS + Docker + Nginx

| 对比项 | 阿里云ECS（推荐） | 腾讯云CVM | 华为云ECS | 轻量应用服务器 |
|--------|------------------|----------|----------|--------------|
| 内存 | 2GB+ | 2GB+ | 2GB+ | 2GB+ |
| 带宽 | 3Mbps+ | 3Mbps+ | 3Mbps+ | 3Mbps+ |
| 域名备案 | 需备案 | 需备案 | 需备案 | 需备案 |
| 适合 | 生产环境 | 生产环境 | 生产环境 | 个人/测试 |

**最低配置要求：**
- CPU：2 核
- 内存：2GB
- 带宽：3Mbps
- 系统盘：40GB SSD
- 操作系统：Ubuntu 22.04 LTS 或 CentOS 8

**预估费用：**
- 云服务器：约 100-300 元/月
- 域名：约 50-70 元/年
- SSL 证书：免费（Let's Encrypt）
- 总计：约 150-400 元/月

---

## 3. 第一步：购买云服务器

### 3.1 阿里云 ECS 购买流程

1. 访问 [阿里云 ECS 控制台](https://ecs.console.aliyun.com/)
2. 点击 **创建实例**
3. 选择配置：
   - **付费模式**：按量付费（测试）或包年包月（生产）
   - **地域**：选择国内（如华东1-杭州）
   - **实例规格**：2核2G（最低要求）
   - **镜像**：公共镜像 → Ubuntu → 22.04 LTS 64位
   - **系统盘**：40GB ESSD云盘
   - **公网带宽**：3Mbps 按固定带宽
   - **安全组**：开放 22、80、443 端口
4. 勾选 **同意服务协议** → 点击 **创建实例**
5. 设置 root 密码（请牢记）

### 3.2 配置安全组（重要）

购买后务必配置安全组，开放以下端口：

```bash
# 方式一：控制台操作
# 进入 ECS → 安全组 → 配置规则 → 添加入方向规则

# 需要开放的端口：
# 22    - SSH 远程登录
# 80    - HTTP 访问
# 443   - HTTPS 访问
# 3000  - H5 前端（临时调试用，正式环境通过80端口访问）
# 3001  - 管理后台（临时调试用）
# 8080  - 后端API（临时调试用）
# 5432  - PostgreSQL（仅内网访问，建议不要开放公网）
```

### 3.3 本地 SSH 连接测试

```bash
# Windows PowerShell 或 CMD
ssh root@你的服务器公网IP

# 示例
ssh root@47.100.100.100

# 输入购买时设置的密码
# 看到 root@iZxxxx:# 提示符表示连接成功
```

---

## 4. 第二步：购买域名和备案

### 4.1 购买域名

1. 访问 [阿里云域名注册](https://wanwang.aliyun.com/domain/)
2. 搜索想要的域名（如 `sufuwanjia.com`）
3. 选择后缀（推荐 `.com` 或 `.cn`）
4. 完成购买（约 50-70 元/年）

### 4.2 域名备案（必须）

根据中国法律，域名指向国内服务器必须进行 ICP 备案：

1. 登录 [阿里云备案控制台](https://beian.console.aliyun.com/)
2. 点击 **开始备案**
3. 填写信息：
   - 主体信息（个人或企业）
   - 域名信息
   - 服务器信息（选择你购买的 ECS）
4. 上传证件照片
5. 提交审核（阿里云初审 1-2 天，管局审核 7-20 天）

**注意：** 备案期间网站无法通过域名访问，但可通过公网IP临时访问。

### 4.3 域名解析配置

备案完成后，配置 DNS 解析：

```
进入 阿里云域名控制台 → 域名解析 → 添加记录

H5 用户端：
  主机记录：chat
  记录类型：A
  记录值：你的服务器公网IP
  示例：chat.yourdomain.com → 47.100.100.100

管理后台：
  主机记录：admin
  记录类型：A
  记录值：你的服务器公网IP
  示例：admin.yourdomain.com → 47.100.100.100
```

---

## 5. 第三步：服务器环境搭建

### 5.1 SSH 登录服务器

```bash
ssh root@你的服务器公网IP
```

### 5.2 系统更新

```bash
# Ubuntu 22.04
apt update && apt upgrade -y

# CentOS 8
yum update -y
```

### 5.3 安装 Docker

```bash
# Ubuntu 22.04 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 启动 Docker 并设置开机自启
systemctl enable docker
systemctl start docker

# 验证安装
docker --version
# 输出：Docker version 24.x.x

# 安装 Docker Compose
apt install docker-compose-plugin -y
docker compose version
# 输出：Docker Compose version v2.x.x
```

### 5.4 安装 Git

```bash
# Ubuntu
apt install git -y

# CentOS
yum install git -y

git --version
```

### 5.5 安装 Nginx（用于 HTTPS 证书申请）

```bash
apt install nginx -y
systemctl enable nginx
systemctl start nginx
```

---

## 6. 第四步：配置项目文件

### 6.1 上传项目到服务器

```bash
# 方式一：Git 克隆（推荐）
cd /opt
git clone https://github.com/你的用户名/tiny.git
cd tiny

# 方式二：SCP 上传本地项目
# 在本地 Windows PowerShell 执行：
scp -r C:\Users\Elaine\OneDrive\projects-win\tiny root@你的服务器IP:/opt/

# 方式三：使用 WinSCP 图形化上传
# 下载 WinSCP → 连接服务器 → 拖拽上传
```

### 6.2 配置环境变量

```bash
cd /opt/tiny

# 复制环境变量模板
cp .env.example .env

# 编辑配置
nano .env
```

**.env 文件内容：**
```bash
# 数据库密码（请使用强密码）
POSTGRES_DB=support_bot
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YourStrongPassword123!

# 阿里云 DashScope API Key（必填）
# 获取地址：https://dashscope.console.aliyun.com/apiKey
DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# AI模型配置
DASHSCOPE_MODEL=qwen-max
DASHSCOPE_EMBEDDING_MODEL=qwen3.7-text-embedding

# RAG知识库开关
RAG_ENABLED=true
```

### 6.3 配置 Nginx 反向代理（HTTPS 方案）

创建生产环境 Nginx 配置：

```bash
# 创建 Nginx 配置目录
mkdir -p /opt/tiny/nginx

# 创建 H5 前端配置
nano /opt/tiny/nginx/h5.conf
```

**h5.conf 内容：**
```nginx
server {
    listen 80;
    server_name chat.yourdomain.com;

    # HTTP 重定向到 HTTPS
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name chat.yourdomain.com;

    # SSL 证书（稍后配置）
    ssl_certificate /etc/letsencrypt/live/chat.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.yourdomain.com/privkey.pem;

    # HSTS 安全头
    add_header Strict-Transport-Security "max-age=31536000" always;

    root /usr/share/nginx/html;
    index index.html;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript application/xml+rss;

    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # API代理
    location /api/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # AI SSE流式接口
    location /ai/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400s;
    }

    # SPA路由
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
# 创建管理后台配置
nano /opt/tiny/nginx/admin.conf
```

**admin.conf 内容：**
```nginx
server {
    listen 80;
    server_name admin.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name admin.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/admin.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.yourdomain.com/privkey.pem;

    add_header Strict-Transport-Security "max-age=31536000" always;

    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ai/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### 6.4 获取 SSL 证书

```bash
# 安装 certbot
apt install certbot python3-certbot-nginx -y

# 为 H5 域名申请证书
certbot --nginx -d chat.yourdomain.com

# 为管理后台域名申请证书
certbot --nginx -d admin.yourdomain.com

# 自动续期（默认已配置）
certbot renew --dry-run
```

---

## 7. 第五步：部署服务

### 7.1 一键部署（推荐）

```bash
cd /opt/tiny

# 使用 docker-compose 构建并启动所有服务
docker compose -f docker-compose.full.yml up -d --build

# 查看服务状态
docker compose -f docker-compose.full.yml ps

# 查看日志
docker compose -f docker-compose.full.yml logs -f
```

### 7.2 单独构建和启动

```bash
# 构建后端镜像
cd /opt/tiny/chat-bot
docker build -t support-bot-backend:latest .

# 构建 H5 前端镜像
cd /opt/tiny/chat-bot-h5
docker build -t support-bot-h5:latest .

# 构建管理后台镜像
cd /opt/tiny/chat-bot-admin
docker build -t support-bot-admin:latest .

# 使用 compose 启动
cd /opt/tiny
docker compose -f docker-compose.full.yml up -d
```

### 7.3 验证服务是否启动

```bash
# 检查容器状态（所有服务应为 healthy 或 running）
docker compose -f docker-compose.full.yml ps

# 检查后端 API 是否正常
curl http://localhost:8080/ai/assistant_app/chat/with-tools?message=你好&chatId=test

# 检查 H5 前端
curl http://localhost:3000

# 检查管理后台
curl http://localhost:3001
```

---

## 8. 第六步：配置Nginx反向代理和HTTPS

### 8.1 配置 Docker 容器的端口映射

编辑 `docker-compose.full.yml`，修改端口映射为仅绑定本地（安全）：

```yaml
# 修改 H5 前端端口
h5-frontend:
  ports:
    - "127.0.0.1:3000:80"    # 只允许本地访问

# 修改管理后台端口  
admin-frontend:
  ports:
    - "127.0.0.1:3001:80"    # 只允许本地访问
```

### 8.2 配置 Nginx 主配置

```bash
# 备份原配置
cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak

# 编辑主配置
nano /etc/nginx/nginx.conf
```

**nginx.conf 主要内容：**
```nginx
user www-data;
worker_processes auto;
pid /run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    # 访问日志
    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log;

    # 基本优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    server_tokens off;

    # Gzip
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript application/xml+rss;

    # 虚拟主机配置
    include /opt/tiny/nginx/h5.conf;
    include /opt/tiny/nginx/admin.conf;
}
```

### 8.3 测试并重载 Nginx

```bash
# 测试配置语法
nginx -t

# 重载配置
systemctl reload nginx

# 如果出错，查看日志
tail -f /var/log/nginx/error.log
```

### 8.4 防火墙配置

```bash
# Ubuntu UFW
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status

# CentOS firewalld
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --reload
```

---

## 9. 第七步：验证部署

### 9.1 功能验证清单

```bash
# 1. H5 前端访问
# 浏览器打开：https://chat.yourdomain.com
# 预期：看到聊天界面，可以发送消息

# 2. 管理后台访问
# 浏览器打开：https://admin.yourdomain.com
# 预期：看到登录页面

# 3. API 直接测试
curl -s https://chat.yourdomain.com/ai/assistant_app/chat/with-tools?message=你好&chatId=verify1

# 4. 天气工具测试
curl -s https://chat.yourdomain.com/ai/assistant_app/chat/with-tools?message=北京天气&chatId=verify2

# 5. 新闻工具测试
curl -s https://chat.yourdomain.com/ai/assistant_app/chat/with-tools?message=最新新闻&chatId=verify3

# 6. 商品推荐测试
curl -s https://chat.yourdomain.com/ai/assistant_app/chat/with-tools?message=推荐商品&chatId=verify4
```

### 9.2 健康检查

```bash
# 检查所有容器状态
docker compose -f docker-compose.full.yml ps

# 检查后端日志
docker compose -f docker-compose.full.yml logs --tail 100 backend

# 检查数据库连接
docker exec -it support-bot-db psql -U postgres -c "SELECT 1"

# 检查磁盘空间
df -h

# 检查内存
free -h
```

---

## 10. 第八步：常用运维命令

### 10.1 日常管理

```bash
cd /opt/tiny

# 启动所有服务
docker compose -f docker-compose.full.yml up -d

# 停止所有服务
docker compose -f docker-compose.full.yml down

# 重启单个服务
docker compose -f docker-compose.full.yml restart backend

# 查看日志
docker compose -f docker-compose.full.yml logs -f backend
docker compose -f docker-compose.full.yml logs -f h5-frontend
docker compose -f docker-compose.full.yml logs -f postgres

# 查看资源使用
docker stats
```

### 10.2 更新部署

```bash
cd /opt/tiny

# 拉取最新代码
git pull origin main

# 重新构建并启动
docker compose -f docker-compose.full.yml up -d --build

# 清理旧镜像
docker image prune -f

# 数据库迁移（如需要）
docker exec -it support-bot-db psql -U postgres -d support_bot
# 然后执行 SQL 语句
```

### 10.3 数据备份

```bash
# 数据库备份
docker exec support-bot-db pg_dump -U postgres support_bot > /opt/backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
cat /opt/backup_YYYYMMDD_HHMMSS.sql | docker exec -i support-bot-db psql -U postgres -d support_bot

# 设置定时备份
crontab -e
# 添加：每天凌晨3点自动备份
0 3 * * * docker exec support-bot-db pg_dump -U postgres support_bot > /opt/backup_$(date +\%Y\%m\%d).sql
```

### 10.4 SSL 证书续期

```bash
# 查看证书有效期
certbot certificates

# 手动续期
certbot renew

# 自动续期测试
certbot renew --dry-run
```

---

## 11. 常见问题排查

### Q1：访问域名显示 502 Bad Gateway

**原因：** Docker 容器未启动或后端服务异常

```bash
# 检查容器状态
docker compose -f docker-compose.full.yml ps

# 查看后端日志
docker compose -f docker-compose.full.yml logs backend

# 重启服务
docker compose -f docker-compose.full.yml restart
```

### Q2：AI 回复显示"网络开小差"

**原因：** 后端连接 DashScope API 失败

```bash
# 检查 API Key 配置
cat /opt/tiny/.env | grep DASHSCOPE

# 测试 API Key 是否有效
curl -X POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen-max","input":{"messages":[{"role":"user","content":"你好"}]}}'

# 如果返回 200 说明 Key 有效
# 如果返回 401/403 说明 Key 无效或过期
```

### Q3：数据库连接失败

```bash
# 检查 PostgreSQL 容器状态
docker compose -f docker-compose.full.yml ps postgres

# 查看数据库日志
docker compose -f docker-compose.full.yml logs postgres

# 测试数据库连接
docker exec -it support-bot-db psql -U postgres -d support_bot

# 如果密码错误，重置密码
docker exec -it support-bot-db psql -U postgres -c "ALTER USER postgres PASSWORD 'NewPassword123!';"
```

### Q4：SSE 流式响应中断

**原因：** Nginx 缓冲或超时设置问题

```bash
# 检查 Nginx 配置中的 SSE 设置
grep -A5 "/ai/" /opt/tiny/nginx/h5.conf

# 确保包含以下配置：
# proxy_buffering off;
# proxy_cache off;
# proxy_read_timeout 86400s;
# chunked_transfer_encoding on;

# 重载 Nginx
nginx -t && systemctl reload nginx
```

### Q5：HTTPS 证书获取失败

```bash
# 检查 DNS 解析
dig chat.yourdomain.com

# 检查端口是否开放
netstat -tlnp | grep -E '80|443'

# 使用备用方式获取证书
certbot certonly --nginx -d chat.yourdomain.com -d admin.yourdomain.com

# 或使用 webroot 方式
certbot certonly --webroot -w /var/www/html -d chat.yourdomain.com
```

### Q6：存储空间不足

```bash
# 检查磁盘使用
df -h

# 清理 Docker 资源
docker system prune -a
docker volume prune

# 清理日志
find /var/log -name "*.log" -size +100M -exec truncate -s 0 {} \;
```

### Q7：如何临时使用 IP 访问（备案期间）

```bash
# 修改 Nginx 配置，临时监听 IP
# 在 h5.conf 中添加：
server {
    listen 80;
    server_name _;  # 接受所有域名

    location /api/ {
        proxy_pass http://127.0.0.1:3000;
    }
    location /ai/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_buffering off;
    }
    location / {
        proxy_pass http://127.0.0.1:3000;
    }
}

# 注意：备案完成后必须换回域名配置
```

---

## 附录 A：一键部署脚本

创建 `/opt/tiny/deploy.sh`：

```bash
#!/bin/bash
set -e

echo "=== 苏福万家智能客服部署脚本 ==="

# 检查环境
command -v docker >/dev/null 2 || { echo "请先安装 Docker"; exit 1; }
command -v docker compose >/dev/null 2 || { echo "请先安装 Docker Compose"; exit 1; }

# 检查配置文件
[ -f .env ] || { echo "请创建 .env 配置文件"; exit 1; }

# 构建并启动
echo "正在构建镜像..."
docker compose -f docker-compose.full.yml build --no-cache

echo "正在启动服务..."
docker compose -f docker-compose.full.yml up -d

# 等待服务就绪
echo "等待服务启动..."
sleep 15

# 健康检查
echo "检查服务状态..."
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "后端启动中..."

echo ""
echo "=== 部署完成 ==="
echo "H5 前端：    http://localhost:3000"
echo "管理后台：   http://localhost:3001"
echo "后端 API：   http://localhost:8080"
echo "数据库：     localhost:5432"
echo ""
echo "请配置 Nginx 反向代理以启用公网访问"
```

使用：
```bash
chmod +x deploy.sh
./deploy.sh
```

## 附录 B：项目文件清单

```
tiny/
├── chat-bot/                    # 后端（Spring Boot）
│   ├── Dockerfile              # 后端 Docker 构建
│   ├── pom.xml                 # Maven 依赖
│   └── src/main/resources/
│       ├── application.yml     # 本地开发配置
│       └── application-prod.yml # 生产配置
├── chat-bot-h5/                 # H5 前端（用户端）
│   ├── Dockerfile              # H5 Docker 构建
│   ├── nginx.conf              # H5 Nginx 配置
│   ├── package.json
│   └── src/                    # Vue 3 源码
├── chat-bot-admin/              # 管理后台
│   ├── Dockerfile              # Admin Docker 构建
│   ├── nginx.conf              # Admin Nginx 配置
│   ├── package.json
│   └── src/                    # Vue 3 源码
├── chat-bot-deploy/             # 部署配置
│   ├── Dockerfile              # 后端部署 Dockerfile
│   ├── application.yml         # 部署版基础配置
│   └── application-prod.yml    # 部署版生产配置
├── docker-compose.full.yml     # 完整编排文件（推荐）
├── docker-compose.yml          # 基础编排文件
├── .env.example                # 环境变量模板
├── .env                        # 实际配置（需自行创建）
├── nginx/                      # Nginx 站点配置
│   ├── h5.conf                 # H5 站点配置
│   └── admin.conf              # Admin 站点配置
└── DEPLOYMENT_GUIDE.md         # 本文档
```

---

## 附录 C：免费/低成本替代方案

如果预算有限，可以考虑以下免费或低成本方案：

### 方案一：阿里云免费试用
- 新用户可享 3 个月免费 ECS
- 访问：https://free.aliyun.com/

### 方案二：腾讯云轻量应用服务器
- 新用户 1核2G 约 50 元/年
- 访问：https://cloud.tencent.com/product/lighthouse

### 方案三：GitHub Pages + Vercel（纯前端）
- H5 前端可部署到 GitHub Pages 或 Vercel（免费）
- 后端需要单独部署（如 Railway、Render 免费额度）
- 优点：零成本、免运维
- 缺点：国内访问速度可能较慢

### 方案四：使用 Cloudflare Tunnel
- 无需购买服务器公网 IP
- 通过 Cloudflare 暴露本地服务
- 免费额度足够小型应用使用
- 配置教程：https://developers.cloudflare.com/cloudflare-one/

---

## 技术支持

如果在部署过程中遇到问题：
1. 查看本文档的 [常见问题排查](#11-常见问题排查) 章节
2. 查看各服务的日志文件
3. 使用 `docker compose logs` 命令排查
4. 提交 Issue 到项目仓库

---

**祝部署顺利！**
