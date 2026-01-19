package com.example.langchain4j.examples;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.util.Scanner;

/**
 * 示例 4：记忆功能（多轮对话）
 * 
 * 演示如何让 AI 记住之前的对话内容，实现真正的多轮对话。
 * 
 * LangChain4j 提供了多种记忆实现：
 * 1. MessageWindowChatMemory - 保留最近 N 条消息
 * 2. TokenWindowChatMemory - 基于 token 数量限制
 */
public class MemoryChatExample {

    // 带记忆的助手接口
    interface ChatAssistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        // 创建模型
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("Qwen2.5-VL-7B-Instruct-local:latest")
                .temperature(0.7)
                .build();

        // 创建聊天记忆 - 保留最近10轮对话
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        // 创建带记忆的 AI 服务
        ChatAssistant assistant = AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();

        System.out.println("=== 记忆功能示例（多轮对话）===");
        System.out.println("AI 会记住你之前说的话！");
        System.out.println("输入 'quit' 退出\n");

        // 预设几轮对话来演示记忆功能
        String[] demoQuestions = {
                "我叫小明，今年25岁，是一名Java程序员。",
                "你还记得我叫什么名字吗？",
                "我的职业是什么？",
                "你能总结一下你知道的关于我的信息吗？"
        };

        System.out.println("--- 自动演示多轮对话 ---\n");

        for (String question : demoQuestions) {
            System.out.println("用户: " + question);
            String response = assistant.chat(question);
            System.out.println("AI: " + response);
            System.out.println();
        }

        System.out.println("--- 进入交互模式 ---");
        System.out.println("（AI 仍然记得之前的对话内容）\n");

        // 交互式对话
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你: ");
            String userInput = scanner.nextLine();

            if ("quit".equalsIgnoreCase(userInput.trim())) {
                System.out.println("再见！");
                break;
            }

            if (userInput.trim().isEmpty()) {
                continue;
            }

            String response = assistant.chat(userInput);
            System.out.println("AI: " + response);
            System.out.println();
        }

        scanner.close();
    }
}
