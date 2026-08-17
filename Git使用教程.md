# Git 使用教程 - 从零开始管理你的项目

> 本教程适用于 Git 初学者，帮助你在多台电脑上高效管理和同步项目代码。

---

## 📋 目录

1. [Git 简介](#git-简介)
2. [核心概念](#核心概念)
3. [基础配置](#基础配置)
4. [常用命令速查](#常用命令速查)
5. [实战流程：首次推送项目到 GitHub](#实战流程首次推送项目到-github)
6. [实战流程：在另一台电脑上拉取项目](#实战流程在另一台电脑上拉取项目)
7. [实战流程：日常开发与同步](#实战流程日常开发与同步)
8. [Cloudflare Tunnel 配置](#cloudflare-tunnel-配置)
9. [常见问题解答](#常见问题解答)

---

## Git 简介

**Git** 是世界上最流行的分布式版本控制系统，由 Linux 之父 Linus Torvalds 开发。

### 为什么要使用 Git？

| 优点 | 说明 |
|------|------|
| 📜 版本历史 | 记录每次代码变更，可随时回溯 |
| 🔄 多人协作 | 多人同时开发，互不干扰 |
| 💾 分布式 | 每个开发者都有完整的仓库副本 |
| 🚀 高效 | 提交、切换分支等操作都非常快速 |
| 🔒 安全 | 数据完整性保护，防止丢失 |

### Git 工作流程示意

```
你的电脑                    GitHub（远程仓库）
┌──────────┐               ┌──────────┐
│  工作区   │  ── commit ──▶ │          │
│ (文件)    │               │ 远程仓库  │
│          │  ◀── pull ──  │          │
└──────────┘               └──────────┘
     │                          │
     ▼                          ▲
┌──────────┐               ┌──────────┐
│ 暂存区    │  ── push ──▶  │          │
│(staged)  │               │          │
└──────────┘               └──────────┘
```

---

## 核心概念

### 1. 仓库 (Repository)
存放项目代码的地方，分为：
- **本地仓库**：存在于你电脑上的 `.git` 隐藏文件夹
- **远程仓库**：托管在 GitHub 等服务器上的仓库

### 2. 工作区 (Working Directory)
你电脑里能看到的项目文件夹，比如 `D:\projects\tiny`

### 3. 暂存区 (Staging Area/Index)
文件修改后，通过 `git add` 放入暂存区，准备提交

### 4. 提交 (Commit)
一次代码快照，类似存档点，可以随时回退

### 5. 分支 (Branch)
代码的平行宇宙，互不影响。主分支通常叫 `main` 或 `master`

### 6. 标签 (Tag)
重要的提交点标记，如 v1.0.0、v2.0.0

---

## 基础配置

### 1. 首次配置（只需做一次）

```bash
# 设置用户名（会显示在每次提交记录中）
git config --global user.name "你的名字"

# 设置邮箱（建议使用 GitHub 注册邮箱）
git config --global user.email "your-email@example.com"

# 设置默认编辑器（可选）
git config --global core.editor "notepad"

# 配置换行符（Windows 系统推荐）
git config --global core.autocrlf true
```

### 2. 查看当前配置

```bash
# 查看所有配置
git config --list

# 查看特定配置
git config user.name
git config user.email
```

---

## 常用命令速查

### 📁 仓库操作

| 命令 | 说明 |
|------|------|
| `git init` | 初始化新仓库 |
| `git clone <url>` | 克隆远程仓库到本地 |
| `git remote -v` | 查看远程仓库信息 |
| `git remote add origin <url>` | 添加远程仓库 |

### 📝 提交操作

| 命令 | 说明 |
|------|------|
| `git status` | 查看当前状态（最常用） |
| `git add <file>` | 添加文件到暂存区 |
| `git add .` | 添加所有变更文件 |
| `git commit -m "说明"` | 提交并添加说明 |
| `git commit -am "说明"` | 添加+提交（仅限已跟踪文件） |

### 🌿 分支操作

| 命令 | 说明 |
|------|------|
| `git branch` | 查看本地分支 |
| `git branch <name>` | 创建新分支 |
| `git checkout <branch>` | 切换分支（旧语法） |
| `git switch <branch>` | 切换分支（新语法，推荐） |
| `git checkout -b <branch>` | 创建并切换到新分支 |
| `git merge <branch>` | 合并分支到当前分支 |
| `git branch -d <branch>` | 删除分支 |

### 🔄 同步操作

| 命令 | 说明 |
|------|------|
| `git fetch` | 拉取远程更新（不合并） |
| `git pull` | 拉取并合并远程更新 |
| `git push` | 推送到远程仓库 |
| `git push -u origin main` | 首次推送并设置上游分支 |

### 🔍 查看操作

| 命令 | 说明 |
|------|------|
| `git log` | 查看提交历史 |
| `git log --oneline` | 简洁版历史记录 |
| `git diff` | 查看未暂存的修改 |
| `git diff --staged` | 查看已暂存的修改 |
| `git show <commit>` | 查看某次提交详情 |

### ↩️ 撤销操作

| 命令 | 说明 |
|------|------|
| `git checkout -- <file>` | 撤销未提交的修改 |
| `git restore <file>` | 撤销修改（新语法） |
| `git reset HEAD <file>` | 取消暂存 |
| `git reset --soft HEAD~1` | 撤销上次提交（保留修改） |
| `git reset --hard HEAD~1` | 撤销上次提交（丢弃修改）⚠️ |

---

## 实战流程：首次推送项目到 GitHub

### 步骤 1：在 GitHub 创建仓库

1. 登录 [github.com](https://github.com)
2. 点击右上角 **+** → **New repository**
3. 填写：
   - **Repository name**: `ai-bot-project`（或你喜欢的名称）
   - **Description**: 可选，项目描述
   - **Private/Public**: 选择 Private（私有）或 Public（公开）
4. **不要**勾选 "Initialize this repository with a README"
5. 点击 **Create repository**

### 步骤 2：本地初始化并推送

```bash
# 进入项目目录
cd D:\projects\tiny

# 初始化 Git 仓库（如果还没做）
git init

# 添加所有文件到暂存区
git add .

# 首次提交
git commit -m "初始化项目：AI智能客服系统"

# 添加远程仓库（替换为你的 GitHub 用户名和仓库名）
git remote add origin https://github.com/你的用户名/ai-bot-project.git

# 推送到 GitHub
git push -u origin main
```

### 步骤 3：验证推送成功

1. 打开你的 GitHub 仓库页面
2. 刷新页面，应该能看到项目文件
3. 恭喜！🎉 你的项目已经备份到云端

---

## 实战流程：在另一台电脑上拉取项目

### 步骤 1：安装 Git

访问 [git-scm.com](https://git-scm.com) 下载安装

### 步骤 2：克隆项目

```bash
# 选择一个工作目录
cd D:\projects

# 克隆仓库（替换为你的仓库地址）
git clone https://github.com/你的用户名/ai-bot-project.git

# 进入项目目录
cd ai-bot-project
```

### 步骤 3：运行项目

```bash
# 后端
cd chat-bot
# Windows: 设置 Java 17 环境变量后启动
$env:JAVA_HOME = "D:\software\JDK"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:DASHSCOPE_API_KEY = "你的API密钥"
mvn spring-boot:run

# 管理后台（新终端）
cd ../chat-bot-admin
npm install  # 首次运行需要
npm run dev

# H5 移动端（新终端）
cd ../chat-bot-h5
npm install  # 首次运行需要
npm run dev
```

---

## 实战流程：日常开发与同步

### 修改代码后同步到 GitHub

```bash
# 1. 查看修改了哪些文件
git status

# 2. 添加修改的文件
git add .

# 3. 提交并写清楚修改内容
git commit -m "feat: 添加新的客服对话功能"

# 4. 推送到 GitHub
git push
```

### 在另一台电脑同步最新代码

```bash
# 拉取最新更新
git pull
```

### 推荐的提交说明格式

```
<type>: <description>

[type] 类型：
  feat:     新功能
  fix:      修复 bug
  docs:     文档更新
  style:    代码格式（不影响功能）
  refactor: 重构
  test:     测试相关
  chore:    构建/工具变更

示例：
  feat: 添加用户登录功能
  fix: 修复聊天消息重复发送的问题
  docs: 更新 API 文档
```

---

## Cloudflare Tunnel 配置

### 简介

Cloudflare Tunnel 可以让你在没有公网 IP 或域名的情况下，通过 Cloudflare 的网络将本地服务暴露到互联网。

### 前提条件

- 拥有 [Cloudflare](https://dash.cloudflare.com) 账号
- 已将自己的域名（例如 `example.com`）的 DNS 托管到 Cloudflare
- 本地已安装 cloudflared 客户端

### 步骤 1：安装 cloudflared

**方法一：直接下载（推荐）**

1. 访问 [cloudflared 下载页](https://github.com/cloudflare/cloudflared/releases/latest)
2. 下载 `cloudflared-windows-amd64.exe`
3. 将文件放到 `D:\software\cloudflared.exe`（或任意位置）
4. 将该路径添加到系统环境变量 PATH 中

**方法二：使用 winget**

```powershell
winget install Cloudflare.cloudflared
```

### 步骤 2：登录 Cloudflare

```bash
cloudflared tunnel login
```

执行后会打开浏览器，选择你自己的域名（例如 `example.com`）进行授权。

### 步骤 3：创建 Tunnel

```bash
# 创建隧道
cloudflared tunnel create aibot-tunnel

# 记录输出的隧道 UUID，例如：
# A1B2C3D4-E5F6-7890-ABCD-EF1234567890
```

### 步骤 4：配置 DNS 记录

```bash
# 为 H5 页面配置域名
cloudflared tunnel route dns aibot-tunnel h5.example.com

# 为管理后台配置域名
cloudflared tunnel route dns aibot-tunnel admin.example.com
```

### 步骤 5：更新配置文件

项目中已提供 `.cloudflared/config.yml` 模板，需要将隧道 UUID 替换进去：

```yaml
tunnel: aibot-tunnel
credentials-file: C:\Users\你的用户名\.cloudflared\<TUNNEL-UUID>.json

ingress:
  - hostname: h5.example.com
    service: http://localhost:5173

  - hostname: admin.example.com
    service: http://localhost:5174

  - service: http://localhost:8081
```

### 步骤 6：启动 Tunnel

```bash
# 方式一：使用项目提供的脚本（Windows）
start-tunnel.bat

# 方式二：命令行直接启动
cloudflared tunnel --config .cloudflared/config.yml run
```

### 步骤 7：验证

访问以下地址验证：
- H5 页面：https://h5.example.com
- 管理后台：https://admin.example.com

### 开机自启（可选）

可以将 cloudflared 注册为 Windows 服务：

```bash
# 安装为系统服务
cloudflared service install

# 或使用 NSSM 等工具创建服务
```

---

## 常见问题解答

### Q1: git push 时提示需要认证怎么办？

**A**: GitHub 已不再支持密码认证，需要使用 Personal Access Token：

1. 访问 [github.com/settings/tokens](https://github.com/settings/tokens)
2. 点击 **Generate new token (classic)**
3. 选择权限：`repo`（完整的仓库访问）
4. 生成后复制 Token
5. 推送时使用 Token 作为密码

```bash
# 推送时会提示输入用户名和密码
# 用户名：你的 GitHub 用户名
# 密码：刚才生成的 Token（不是 GitHub 登录密码）
git push origin main

# 或配置远程 URL 使用 Token（更方便）
git remote set-url origin https://用户名:Token@github.com/用户名/仓库名.git
```

### Q2: 如何撤销已提交的代码？

**A**: 根据情况选择：

```bash
# 场景 1：撤销最近一次提交，但保留修改
git reset --soft HEAD~1

# 场景 2：撤销最近一次提交，丢弃修改 ⚠️
git reset --hard HEAD~1

# 场景 3：修改最近一次提交的信息
git commit --amend -m "新的提交说明"
```

### Q3: 两台电脑如何保持同步？

**A**: 

```bash
# 电脑 A（推送）
git add .
git commit -m "更新说明"
git push

# 电脑 B（拉取）
git pull
```

建议养成习惯：**每次开始工作前先 `git pull`，每次完成工作后 `git push`**

### Q4: 如何查看修改历史？

**A**: 

```bash
# 查看完整历史
git log

# 简洁模式（推荐）
git log --oneline

# 查看最近 5 条
git log --oneline -5

# 查看某个文件的修改历史
git log --oneline chat-bot/src/main/resources/application.yml
```

### Q5: 不小心把不该提交的文件加进去了怎么办？

**A**: 

```bash
# 从暂存区移除文件（不会删除文件）
git reset HEAD <文件路径>

# 示例
git reset HEAD .env
git reset HEAD node_modules/
```

同时确保 `.gitignore` 文件正确配置，避免误提交。

### Q6: 多个分支怎么管理？

**A**: 

```bash
# 查看所有分支
git branch

# 创建新分支开发功能
git checkout -b feature/new-chat-ui

# 切换回主分支
git checkout main

# 合并功能分支到主分支
git checkout main
git merge feature/new-chat-ui

# 删除已合并的分支
git branch -d feature/new-chat-ui

# 推送分支到远程
git push origin feature/new-chat-ui
```

### Q7: Cloudflare Tunnel 连接断开怎么办？

**A**: 

```bash
# 查看隧道状态
cloudflared tunnel info aibot-tunnel

# 重启隧道
cloudflared tunnel --config .cloudflared/config.yml run

# 如果持续断开，检查：
# 1. 网络连接是否稳定
# 2. 本地服务是否正常运行
# 3. 重启 cloudflared 服务
```

### Q8: 如何备份整个项目？

**A**: 

```bash
# 方法一：使用 git bundle 打包
git bundle create backup.bundle --all

# 恢复备份
git clone backup.bundle new-project

# 方法二：直接复制文件夹（简单但不包含历史）
# 直接复制项目文件夹到备份位置
```

---

## 📚 推荐学习资源

| 资源 | 链接 | 说明 |
|------|------|------|
| 官方文档 | [git-scm.com/docs](https://git-scm.com/docs) | Git 完整文档 |
| 菜鸟教程 | [runoob.com/git](https://www.runoob.com/git/git-tutorial.html) | 中文入门教程 |
| 可视化练习 | [learngitbranching.js.org](https://learngitbranching.js.org) | 交互式 Git 练习 |
| Git 游戏 | [githowto.com](https://githowto.com) | 游戏化学习 |

---

## 🎯 快速参考卡片

```
┌─────────────────────────────────────────────────────────┐
│                 Git 快速参考卡片                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  初始化:  git init                                      │
│  克隆:    git clone <url>                               │
│                                                         │
│  查看状态: git status                                   │
│  添加文件: git add .                                    │
│  提交:     git commit -m "说明"                         │
│  推送:     git push                                     │
│  拉取:     git pull                                     │
│                                                         │
│  分支:     git branch <name>                            │
│  切换:     git switch <branch>                          │
│  合并:     git merge <branch>                           │
│                                                         │
│  日志:     git log --oneline                            │
│  撤销:     git restore <file>                           │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ 检查清单

- [ ] GitHub 账号已注册
- [ ] 本地 Git 已安装
- [ ] 用户名和邮箱已配置
- [ ] 项目已初始化并推送到 GitHub
- [ ] Cloudflare Tunnel 已配置
- [ ] 域名 DNS 记录已添加
- [ ] 本地服务可通过公网访问
- [ ] 知道如何在另一台电脑上拉取项目
- [ ] 了解日常同步流程

---

**祝您使用愉快！🚀**

如有问题，可随时查阅本文档或访问 [Cloudflare 文档](https://developers.cloudflare.com) 和 [Git 文档](https://git-scm.com/docs)。
