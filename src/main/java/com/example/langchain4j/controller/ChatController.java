package com.example.langchain4j.controller;

import com.example.langchain4j.service.KnowledgeBaseService;
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
    private final KnowledgeBaseService knowledgeBaseService;

    public ChatController(ChatLanguageModel chatLanguageModel, KnowledgeBaseService knowledgeBaseService) {
        this.chatLanguageModel = chatLanguageModel;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 简单对话接口 (不带 RAG)
     *
     * 使用方式: GET /api/chat?message=你好
     */
    @GetMapping
    public String chat(@RequestParam String message) {
        return chatLanguageModel.generate(message);
    }

    /**
     * RAG 对话接口 (带知识库)
     *
     * 使用方式: GET /api/chat/rag?message=LangChain4j有哪些特性
     */
    @GetMapping("/rag")
    public String chatWithRag(@RequestParam String message) {
        return knowledgeBaseService.chat(message);
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
