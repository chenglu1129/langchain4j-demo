package com.example.langchain4j.examples;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 示例 3：AI Services（接口化）
 * 
 * 这是 LangChain4j 最强大的特性之一！
 * 你只需要定义一个 Java 接口，LangChain4j 会自动实现它。
 * 
 * 特点：
 * 1. 类型安全 - 返回值可以是任意类型
 * 2. 可测试 - 可以轻松 mock
 * 3. 清晰易读 - 用接口定义 AI 能力
 */
public class AiServiceExample {

    // 定义一个 AI 助手接口
    interface Assistant {

        @SystemMessage("你是一个专业的程序员助手，回答要简洁明了。")
        String chat(String message);
    }

    // 定义一个翻译接口
    interface Translator {

        @SystemMessage("你是一个专业的翻译，请将用户的输入翻译成目标语言。只返回翻译结果，不要有其他内容。")
        @UserMessage("请将以下内容翻译成{{language}}：{{text}}")
        String translate(@V("text") String text, @V("language") String targetLanguage);
    }

    // 定义一个情感分析接口
    interface SentimentAnalyzer {

        @SystemMessage("你是一个情感分析专家。分析用户输入的情感，返回：POSITIVE、NEGATIVE 或 NEUTRAL")
        @UserMessage("分析以下文本的情感：{{it}}")
        String analyze(String text);
    }

    public static void main(String[] args) {
        // 创建模型
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("Qwen2.5-VL-7B-Instruct-local:latest")
                .temperature(0.3) // 降低温度，使输出更稳定
                .build();

        System.out.println("=== AI Services 示例 ===\n");

        // 示例 1：简单助手
        System.out.println("--- 1. 程序员助手 ---");
        Assistant assistant = AiServices.create(Assistant.class, model);
        String answer = assistant.chat("什么是单例模式？");
        System.out.println("问题：什么是单例模式？");
        System.out.println("回答：" + answer);

        // 示例 2：翻译服务
        System.out.println("\n--- 2. 翻译服务 ---");
        Translator translator = AiServices.create(Translator.class, model);
        String translated = translator.translate("Hello, how are you today?", "中文");
        System.out.println("原文：Hello, how are you today?");
        System.out.println("翻译：" + translated);

        // 示例 3：情感分析
        System.out.println("\n--- 3. 情感分析 ---");
        SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, model);

        String[] texts = {
                "这个产品太棒了，我非常喜欢！",
                "服务态度很差，非常失望。",
                "今天天气还可以。"
        };

        for (String text : texts) {
            String sentiment = analyzer.analyze(text);
            System.out.println("文本：" + text);
            System.out.println("情感：" + sentiment);
            System.out.println();
        }
    }
}
