package com.example.langchain4j.examples;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例 5：工具调用（Function Calling）
 * 
 * 演示如何让 AI 调用自定义的 Java 方法。
 * 这是构建 Agent 的基础能力！
 * 
 * 工作原理：
 * 1. 定义带 @Tool 注解的方法
 * 2. AI 识别用户意图，决定调用哪个工具
 * 3. LangChain4j 自动执行工具并返回结果
 */
public class ToolCallingExample {

    // 定义工具类
    static class Calculator {

        @Tool("计算两个数的和")
        public double add(double a, double b) {
            System.out.println("[工具调用] 计算加法: " + a + " + " + b);
            return a + b;
        }

        @Tool("计算两个数的差")
        public double subtract(double a, double b) {
            System.out.println("[工具调用] 计算减法: " + a + " - " + b);
            return a - b;
        }

        @Tool("计算两个数的乘积")
        public double multiply(double a, double b) {
            System.out.println("[工具调用] 计算乘法: " + a + " × " + b);
            return a * b;
        }

        @Tool("计算两个数的商")
        public double divide(double a, double b) {
            System.out.println("[工具调用] 计算除法: " + a + " ÷ " + b);
            if (b == 0) {
                throw new IllegalArgumentException("除数不能为零");
            }
            return a / b;
        }
    }

    // 定义更有趣的工具
    static class UtilityTools {

        @Tool("获取当前时间")
        public String getCurrentTime() {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("[工具调用] 获取当前时间: " + time);
            return time;
        }

        @Tool("将文本转换为大写")
        public String toUpperCase(String text) {
            System.out.println("[工具调用] 转换大写: " + text);
            return text.toUpperCase();
        }

        @Tool("计算字符串的长度")
        public int getStringLength(String text) {
            System.out.println("[工具调用] 计算长度: " + text);
            return text.length();
        }
    }

    // 定义 AI 助手接口
    interface ToolAssistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        // 创建模型
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("Qwen2.5-VL-7B-Instruct-local:latest")
                .temperature(0.0) // 工具调用建议使用低温度
                .build();

        // 创建工具实例
        Calculator calculator = new Calculator();
        UtilityTools utilityTools = new UtilityTools();

        // 创建带工具的 AI 服务
        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(calculator, utilityTools)
                .build();

        System.out.println("=== 工具调用示例 ===");
        System.out.println("AI 会根据你的问题自动调用合适的工具\n");

        // 测试问题
        String[] questions = {
                "请帮我计算 123 加 456 等于多少？",
                "现在几点了？",
                "请把 'hello world' 转换成大写",
                "25 乘以 4 再除以 5 等于多少？"
        };

        for (String question : questions) {
            System.out.println("----------------------------------------");
            System.out.println("用户: " + question);
            System.out.println();

            try {
                String response = assistant.chat(question);
                System.out.println("AI: " + response);
            } catch (Exception e) {
                System.out.println("错误: " + e.getMessage());
            }

            System.out.println();
        }
    }
}
