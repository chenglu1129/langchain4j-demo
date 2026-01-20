package com.example.langchain4j.service;

/**
 * 知识库服务接口
 * 
 * 支持基于 RAG 的对话功能
 */
public interface KnowledgeBaseService {
    /**
     * 基于知识库进行对话
     * 
     * @param userMessage 用户消息
     * @return AI 响应
     */
    String chat(String userMessage);
}
