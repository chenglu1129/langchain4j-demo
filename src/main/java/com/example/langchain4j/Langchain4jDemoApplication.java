package com.example.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LangChain4j 示例项目启动类
 * 
 * 本项目演示 LangChain4j 的核心特性：
 * 1. 基础对话 - BasicChatExample
 * 2. 流式输出 - StreamingChatExample
 * 3. AI Services - AiServiceExample
 * 4. 记忆功能 - MemoryChatExample
 * 5. 工具调用 - ToolCallingExample
 */
@SpringBootApplication
public class Langchain4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Langchain4jDemoApplication.class, args);
    }
}
