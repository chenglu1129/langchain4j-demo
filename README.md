# LangChain4j Demo Project

这是一个基于 Spring Boot 和 [LangChain4j](https://github.com/langchain4j/langchain4j) 的示例项目，展示了如何在 Java 应用中集成和使用本地大语言模型（LLM）。本项目默认配置使用 [Ollama](https://ollama.com/) 运行本地模型。

## ✨ 特性

- **Spring Boot 3 集成**：基于 Spring Boot 3.2.5 构建。
- **LangChain4j 0.36.x**：使用最新的 LangChain4j 库。
- **Ollama 支持**：通过 Ollama 连接本地运行的大模型。
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
- **Docker**: 用于运行 Ollama（可选，也可以直接安装 Ollama）

## 🚀 快速开始

### 1. 准备 Ollama 环境

本项目依赖 Ollama 提供 LLM 服务。你可以通过 Docker Compose 启动，或者直接在本地安装 Ollama。

**方式 A: 使用 Docker Compose (推荐)**

项目根目录下提供了 `docker-compose.yml` 文件，配置了 GPU 支持（需要 NVIDIA Container Toolkit）。

```bash
docker-compose up -d
```

**方式 B: 本地安装**

请访问 [Ollama 官网](https://ollama.com/download) 下载并安装对应系统的版本。

### 2. 下载模型

项目默认配置的模型名称为 `Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf`。你需要确保 Ollama 中有该模型，或者下载其他模型并修改配置。

```bash
# 下载通义千问模型 (示例使用 qwen2.5:7b，请根据实际情况调整)
ollama pull qwen2.5:7b
```

**⚠️ 注意**：如果你下载的模型名称不是 `Qwen2.5-VL-7B-Instruct-Q4_K_M.gguf`，请务必修改 `src/main/resources/application.yml` 文件中的 `model-name` 配置：

```yaml
langchain4j:
  ollama:
    chat-model:
      model-name: Qwen2.5-VL-7B-Instruct-local:latest  # 修改为你下载的模型名称
    streaming-chat-model:
      model-name: Qwen2.5-VL-7B-Instruct-local:latest
```

### 3. 启动应用

使用 Maven 启动 Spring Boot 应用：

```bash
mvn spring-boot:run
```

应用启动后，默认运行在端口 `8080`。

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
