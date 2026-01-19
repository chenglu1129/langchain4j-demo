package com.example.langchain4j.examples;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

/**
 * 示例 1：基础对话
 * 
 * 演示最简单的 LangChain4j 使用方式：
 * 1. 创建 ChatLanguageModel
 * 2. 调用 generate() 方法获取回复
 */
public class BasicChatExample {

    public static void main(String[] args) {
        // 创建 Ollama Chat Model
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("Qwen2.5-VL-7B-Instruct-local:latest")
                .temperature(0.7)
                .build();

        // 发送消息并获取回复
        String response = model.generate("你好！请用一句话介绍一下你自己。");

        System.out.println("=== 基础对话示例 ===");
        System.out.println("AI 回复: " + response);

        // 再问一个问题
        String response2 = model.generate("Java 和 Python 的主要区别是什么？请简要说明。");
        System.out.println("\nAI 回复: " + response2);
    }
}
