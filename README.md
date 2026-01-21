# LangChain4j Demo Project

这是一个基于 Spring Boot 和 [LangChain4j](https://github.com/langchain4j/langchain4j) 的示例项目，展示了如何在 Java 应用中集成和使用本地大语言模型（LLM）。本项目默认配置使用 [Ollama](https://ollama.com/) 运行本地模型。

## ✨ 特性

- **Spring Boot 3 集成**：基于 Spring Boot 3.2.5 构建。
- **LangChain4j 0.36.x**：使用最新的 LangChain4j 库。
- **Ollama 支持**：通过 Ollama 连接本地运行的大模型。
- **多向量数据库支持**：
    - **PgVector** (推荐，基于 PostgreSQL)
    - **Milvus**
    - **Elasticsearch**
    - **Chroma**
- **RAG (检索增强生成)**：
    - 支持从本地 `src/main/resources/documents` 目录加载文档。
    - **智能增量更新**：系统启动时自动检测新增文件并导入，避免重复处理。
- **配置中心化**：通过 `application.yml` 统一管理所有业务参数和数据库连接。
- **生产级日志**：按等级分类存储，支持按天滚动和文件大小切分。
- **多种交互模式**：
    - 基础对话 (Basic Chat)
    - 流式响应 (Streaming)
    - 记忆功能 (Memory)
    - AI 服务 (AI Service)
    - 工具调用 (Tool Calling)
- **REST API**：提供 HTTP 接口进行对话测试。
- **简单的 Web 界面**：包含一个简单的静态页面用于测试对话。

## 🛠️ 环境要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.x
- **Docker**: 用于运行 Ollama 和向量数据库（推荐）

## 🚀 快速开始

### 1. 准备环境 (Ollama + 向量数据库)

本项目使用 `docker-compose.yml` 编排所有依赖服务，包括 Ollama 和 PostgreSQL (PgVector)。

**启动所有服务**：

```bash
docker-compose up -d
```

这将启动：
- **Ollama**: 本地大模型服务
- **Postgres (PgVector)**: 向量数据库服务 (端口 5432)
- **Web UI**: (可选) Open WebUI

### 2. 下载模型

项目默认配置的模型名称为 `Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf`。

```bash
# 示例：下载通义千问模型
ollama pull qwen2.5:7b
```

**⚠️ 注意**：请务必修改 `src/main/resources/application.yml` 文件中的 `model-name` 配置与你下载的模型一致。

### 3. 配置应用

所有配置均集中在 `src/main/resources/application.yml` 中。

**切换向量数据库**：
修改 `spring.profiles.active` 属性：

```yaml
spring:
  profiles:
    active: pgvector  # 可选值: pgvector, milvus, elasticsearch, chroma
```

**修改数据库连接与参数**：
直接在 `application.yml` 的 `app` 节点下修改：

```yaml
app:
  embedding:
    dimension: 512              # 向量维度
  document:
    splitter:
      max-segment-size: 300     # 文档切分大小
  vector-store:
    collection-name: langchain4j_vectors
    pgvector:
      host: localhost
      port: 5432
      database: postgres
      user: postgres
      password: postgres
    milvus:
      url: http://localhost:19530
```

### 4. 启动应用

使用 Maven 启动 Spring Boot 应用：

```bash
mvn spring-boot:run
```

应用启动时会自动：
1. 检查 `src/main/resources/documents` 目录下的文档。
2. 将文档切分并向量化。
3. 存入配置的向量数据库中（如果已存在标记文件则跳过）。

## 📝 日志管理

项目配置了生产级日志策略 (`logback-spring.xml`)：

- **日志目录**: `logs/`
- **INFO 日志**: `logs/info-日期-序号.txt` (包含 INFO/WARN)
- **ERROR 日志**: `logs/error-日期-序号.txt` (仅包含 ERROR)
- **滚动策略**:
    - **按天滚动**: 每天生成新文件
    - **按大小切分**: 单个文件超过 10MB 自动切分
    - **保留策略**: 保留最近 30 天，最大占用 3GB

## 📖 使用指南

### Web 界面

打开浏览器访问：[http://localhost:8080](http://localhost:8080)

### API 接口

#### 1. 简单 GET 请求

```http
GET /api/chat?message=你好
```

#### 2. POST 请求

```http
POST /api/chat
Content-Type: application/json

{
    "message": "请介绍一下 LangChain4j"
}
```

### 代码示例

在 `src/main/java/com/example/langchain4j/examples/` 目录下包含多个独立示例：

- `BasicChatExample.java`: 最简单的对话示例。
- `StreamingChatExample.java`: 流式输出示例（打字机效果）。
- `MemoryChatExample.java`: 带有上下文记忆的对话示例。
- `AiServiceExample.java`: 使用声明式接口的高级用法。
- `ToolCallingExample.java`: 让 AI 调用本地 Java 方法（Function Calling）。

## ⚙️ 配置说明

主要配置文件位于 `src/main/resources/application.yml`：

```yaml
langchain4j:
  ollama:
    chat-model:
      base-url: http://localhost:11434 # Ollama 服务地址
      model-name: ...                  # 模型名称
      temperature: 0.7                 # 温度系数 (创造性)
```
