# Support Bot - Docker 部署配置

> ⚠️ **注意**：当前版本默认使用 H2 内存数据库，无需 Docker 即可本地运行。
> Docker 部署仅在生产环境需要时使用。

## 环境变量配置

在运行前，请配置以下环境变量（创建 `.env` 文件）：

```bash
# 阿里云 DashScope API Key（必填）
DASHSCOPE_API_KEY=your-dashscope-api-key

# 可选配置
DASHSCOPE_MODEL=qwen3.7-max
DASHSCOPE_EMBEDDING_MODEL=qwen3.7-text-embedding
SERVER_PORT=8081

# 如需使用 PostgreSQL（可选）
# DB_HOST=localhost
# DB_PORT=5432
# DB_NAME=support_bot
# DB_USERNAME=postgres
# DB_PASSWORD=your-secure-password
```

## 部署步骤

### 1. 克隆项目

```bash
git clone https://github.com/knolonzhou/chat-bot.git
cd chat-bot
```

### 2. 复制部署文件

将本目录下的所有文件复制到项目根目录

### 3. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入真实配置
```

### 4. 启动服务

```bash
# 开发模式（本地开发）
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 生产模式
docker-compose up -d
```

### 5. 查看服务

| 服务 | 地址 |
|------|------|
| API 地址 | http://localhost:8081 |
| API 文档 | http://localhost:8081/doc.html |
| H2 控制台 | http://localhost:8081/h2-console |
| 数据库（PostgreSQL 模式） | localhost:5432 |

### 6. 停止服务

```bash
docker-compose down
```

### 7. 重新构建

```bash
docker-compose build --no-cache chatbot-app
docker-compose up -d chatbot-app
```

## 项目结构

```
chat-bot/
├── Dockerfile              # Docker 构建文件
├── docker-compose.yml      # Docker Compose 编排
├── docker-compose.dev.yml  # 开发环境配置
├── .dockerignore           # Docker 忽略文件
├── .env.example            # 环境变量示例
├── application.yml         # 基础配置
├── application-prod.yml    # 生产环境配置
├── init-db/
│   └── init.sql            # 数据库初始化脚本
└── src/                    # 源代码
```

## 常见问题

### Q: 如何修改端口？
修改 `docker-compose.yml` 中的 `ports` 映射，如 `"8082:8081"`。

### Q: 当前使用什么数据库？
默认使用 H2 内存数据库（PostgreSQL 兼容模式），无需额外安装数据库。如需 PostgreSQL，取消 `application.yml` 中 PostgreSQL datasource 的注释。

### Q: 数据持久化会丢失吗？
H2 内存模式重启后数据会丢失。如需持久化，请使用 PostgreSQL（Docker Volume `chatbot-db-data`）。

### Q: 如何重置数据库？
```bash
docker-compose down -v
docker-compose up -d
```
**警告：这将删除所有数据！**

### Q: 如何查看日志？
```bash
# 应用日志
docker-compose logs -f chatbot-app

# 数据库日志（PostgreSQL 模式）
docker-compose logs -f chatbot-db
```

### Q: API Key 从哪里获取？
访问 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/) 创建 API Key。

### Q: 使用哪些模型？
当前使用免费模型白名单内的模型：
- 对话模型：qwen3.7-max（默认）
- Embedding 模型：qwen3.7-text-embedding（默认）

## 技术栈

- **Java 17** - 编程语言
- **Spring Boot 3.4.4** - 后端框架
- **Spring AI Alibaba 1.0.0-M6.1** - AI 集成框架
- **H2** - 内存数据库（PostgreSQL 兼容模式）
- **SimpleVectorStore** - 内存向量存储
- **阿里云通义千问** - 大语言模型（免费模型白名单）