package com.example.langchain4j.examples;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.output.Response;

import java.util.concurrent.CompletableFuture;

/**
 * 示例 2：流式输出
 * 
 * 演示如何使用 StreamingChatLanguageModel 实现逐字输出效果。
 * 这对于改善用户体验非常重要，尤其是在生成长文本时。
 */
public class StreamingChatExample {

    public static void main(String[] args) throws Exception {
        // 创建 Ollama Streaming Chat Model
        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("Qwen2.5-VL-7B-Instruct-local:latest")
                .temperature(0.7)
                .build();

        System.out.println("=== 流式输出示例 ===");
        System.out.println("AI 正在回复（逐字显示）：");
        System.out.println();

        // 使用 CompletableFuture 等待流式响应完成
        CompletableFuture<Void> future = new CompletableFuture<>();

        // 流式生成
        model.generate("请写一首关于春天的短诗，大约50个字。", new StreamingResponseHandler<AiMessage>() {

            @Override
            public void onNext(String token) {
                // 每收到一个 token 就打印，实现逐字输出效果
                System.out.print(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                System.out.println("\n\n[流式输出完成]");
                future.complete(null);
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("发生错误: " + error.getMessage());
                future.completeExceptionally(error);
            }
        });

        // 等待流式响应完成
        future.get();
    }
}
