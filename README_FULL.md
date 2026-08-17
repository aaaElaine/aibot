# AI智能客服系统 - 完整版

## 项目结构

```
tiny/
├── chat-bot/              # Spring Boot后端
│   ├── src/
│   ├── init-db/           # 数据库初始化脚本
│   ├── Dockerfile
│   └── pom.xml
├── chat-bot-h5/           # Vue 3 + Vant H5对话页面
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── chat-bot-admin/        # Vue 3 + Ant Design Vue 管理后台
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.full.yml
└── .env.example
```

## 功能特性

### H5应用页面
- ✅ 实时对话咨询（同步响应，支持超时和中断）
- ✅ 对话历史记录（本地持久化）
- ✅ 桌面端/移动端响应式适配
- ✅ 用户认证
- ✅ 商品卡片推荐（点击可跳转详情页）
- ✅ 发送/停止按钮切换（可中断对话）
- ✅ AI回复模仿人工客服标准（礼貌、文明、亲和）

### 管理后台
- ✅ 知识库管理（创建、编辑、删除、查看）
- ✅ 文档管理（上传、查看、版本管理）
- ✅ 文档分类管理
- ✅ 知识库质量检测（覆盖度、准确度、完整度）
- ✅ 用户登录认证
- ✅ 数据可视化

### 后端API
- ✅ 知识库CRUD接口
- ✅ 文档上传和向量化
- ✅ 同步对话接口
- ✅ SSE流式对话接口
- ✅ 质量检测接口
- ✅ 用户认证接口
- ✅ 商品搜索接口（支持中文关键词）
- ✅ 免费模型白名单管控

## 快速开始

### 1. 准备环境

确保已安装：
- Node.js 18+ (前端开发)
- JDK 17+ (后端开发)
- 阿里云 DashScope API Key

> **注意**：当前版本使用 H2 内存数据库，无需安装 Docker 或 PostgreSQL。

### 2. 配置环境变量

在 `chat-bot/src/main/resources/application.yml` 中配置：
```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
```

或设置环境变量：
```bash
set DASHSCOPE_API_KEY=your-dashscope-api-key
```

### 3. 启动后端

```bash
cd chat-bot
mvn clean install
mvn spring-boot:run
```

### 4. 启动前端

```bash
# H5 对话页面
cd chat-bot-h5
npm install
npm run dev

# 管理后台
cd chat-bot-admin
npm install
npm run dev
```

### 5. 访问应用

| 应用 | 地址 | 说明 |
|------|------|------|
| H5 对话页面 | http://127.0.0.1:5173 | 智能客服对话界面 |
| 管理后台 | http://127.0.0.1:5174 | 知识库/文档管理 |
| 后端 API | http://127.0.0.1:8081 | REST API 服务 |
| H2 控制台 | http://127.0.0.1:8081/h2-console | 数据库管理（JDBC URL: jdbc:h2:mem:support_bot） |
| API 文档 | http://127.0.0.1:8081/doc.html | Knife4j 接口文档 |

默认管理员账号：
- 用户名: admin
- 密码: admin123

## 开发指南

### 后端开发

```bash
cd chat-bot
mvn clean install
mvn spring-boot:run
```

后端运行在 `http://127.0.0.1:8081`，使用 H2 内存数据库（PostgreSQL 兼容模式）。

### H5前端开发

```bash
cd chat-bot-h5
npm install
npm run dev
```

H5 运行在 `http://127.0.0.1:5173`，通过 Vite 代理转发 API 请求到后端 8081 端口。

### 管理后台开发

```bash
cd chat-bot-admin
npm install
npm run dev
```

管理后台运行在 `http://127.0.0.1:5174`，通过 Vite 代理转发 API 请求到后端 8081 端口。

## API文档

### 认证接口
- POST `/api/auth/login` - 登录
- POST `/api/auth/logout` - 登出
- GET `/api/auth/current` - 获取当前用户

### 知识库接口
- GET `/api/kb/page` - 分页查询知识库
- GET `/api/kb/list` - 查询所有知识库
- GET `/api/kb/{id}` - 查询知识库详情
- POST `/api/kb/create` - 创建知识库
- PUT `/api/kb/update` - 更新知识库
- DELETE `/api/kb/{id}` - 删除知识库

### 文档接口
- GET `/api/document/page` - 分页查询文档
- GET `/api/document/{id}` - 查询文档详情
- POST `/api/document/upload` - 上传文档
- DELETE `/api/document/{id}` - 删除文档
- GET `/api/document/version/{id}` - 查询文档版本历史

### 质量检测接口
- POST `/api/quality/check/{kbId}` - 执行质量检测
- POST `/api/quality/check-all/{kbId}` - 执行全部检测
- GET `/api/quality/history/{kbId}` - 查询检测历史
- GET `/api/quality/latest/{kbId}` - 获取最新检测结果

### 对话接口
- GET `/ai/assistant_app/chat/sse` - SSE流式对话
- GET `/ai/assistant_app/chat/sync` - 同步对话（推荐使用）
- GET `/ai/assistant_app/chat/with-products` - 对话+商品推荐

### 商品接口
- GET `/ai/product/search` - 商品搜索（支持中文关键词）

## 数据库设计

### 核心表
- `sys_user` - 系统用户表
- `knowledge_base` - 知识库表
- `document` - 文档表
- `document_category` - 文档分类表
- `document_version` - 文档版本历史表
- `kb_quality_check` - 知识库质量检测表
- `chat_message` - 聊天消息表
- `product` - 商品表
- `vector_store` - 向量存储表（SimpleVectorStore 内存模式）

## 技术栈

### 后端
- Spring Boot 3.x
- Spring AI Alibaba
- MyBatis Plus
- H2 内存数据库（PostgreSQL 兼容模式）
- SimpleVectorStore（内存向量存储）
- Tika (文档解析)
- 免费模型白名单管控

### H5前端
- Vue 3
- Vite
- TypeScript
- Vant UI
- Axios
- AbortController（对话中断）

### 管理后台
- Vue 3
- Vite
- TypeScript
- Ant Design Vue 4.x
- Pinia
- Axios

## 模型配置

当前使用的免费模型（阿里云 DashScope 免费额度内）：

### 对话模型
- qwen3.7-max（默认）
- qwen3.7-max-2026-05-17
- qwen3.7-max-2026-06-08
- qwen3.7-max-preview
- qwen3.7-plus
- qwen3.7-flash
- qwen3.7-flash-2026-07-15
- deepseek-v4-flash-0731
- glm-5.2
- qwen3.5-ocr

### Embedding 模型
- qwen3.7-text-embedding（默认）

## 部署说明

### 本地开发模式（推荐）

当前版本无需 Docker，直接本地运行：
1. 启动后端（端口 8081）
2. 启动 H5 前端（端口 5173）
3. 启动管理后台（端口 5174）

### Docker 部署

如需 Docker 部署，请参考 [chat-bot-deploy](./chat-bot-deploy) 目录。

## 许可证

MIT License
