package com.example.langchain4j.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.*;

/**
 * Chat REST 控制器
 * 
 * 提供 HTTP 接口来测试 AI 对话功能
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatLanguageModel chatLanguageModel;

    public ChatController(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * 简单对话接口
     * 
     * 使用方式: GET /api/chat?message=你好
     */
    @GetMapping
    public String chat(@RequestParam String message) {
        return chatLanguageModel.generate(message);
    }

    /**
     * POST 对话接口
     * 
     * 使用方式: POST /api/chat
     * Body: { "message": "你好" }
     */
    @PostMapping
    public ChatResponse chatPost(@RequestBody ChatRequest request) {
        System.out.println("收到 POST 请求: " + request.getMessage());
        try {
            String response = chatLanguageModel.generate(request.getMessage());
            System.out.println("模型响应: " + response);
            return new ChatResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("Error: " + e.getMessage());
        }
    }

    // 请求/响应 DTO
    public static class ChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ChatResponse {
        private String response;

        public ChatResponse(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}
