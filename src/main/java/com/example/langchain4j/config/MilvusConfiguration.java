package com.example.langchain4j.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhq.BgeSmallZhQuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
@Profile("milvus") // 仅在 milvus profile 激活时生效
@Slf4j
public class MilvusConfiguration {

    @Bean
    EmbeddingModel embeddingModel() {
        // 使用本地量化的 BGE-Small-ZH 模型，专门针对中文优化，且无需联网
        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    // 向量数据是否已导入的标记文件
    private static final String INGESTION_MARKER_FILE = "data/.milvus_ingested";

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) throws IOException, URISyntaxException {
        // 配置 Milvus 向量数据库连接
        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri("http://localhost:19530")
                .collectionName("langchain4j_vectors")
                .dimension(512) // BGE-Small-ZH 模型的向量维度
                .build();

        Path markerPath = Paths.get(INGESTION_MARKER_FILE);

        // 1. 检查标记文件，如果存在则跳过导入
        if (Files.exists(markerPath)) {
            log.info("检测到标记文件: {}", markerPath.toAbsolutePath());
            log.info("假设 Milvus 已包含向量数据，跳过导入。");
            return embeddingStore;
        }

        // 2. 如果没有标记文件，开始导入数据
        log.info("未找到标记文件，准备导入数据到 Milvus...");

        // 3. 加载文档
        URL url = MilvusConfiguration.class.getClassLoader().getResource("documents");
        if (url == null) {
            log.warn("未找到 documents 目录，跳过知识库加载");
            return embeddingStore;
        }

        // 处理 Windows 路径问题
        Path documentPath;
        try {
            documentPath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            documentPath = Paths.get(url.getPath());
        }

        log.info("正在从以下路径加载文档: {}", documentPath);
        long startTime = System.currentTimeMillis();

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                documentPath,
                new TextDocumentParser());

        log.info("加载了 {} 个文档", documents.size());

        // 4. 将文档切分并存入向量数据库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);

        long duration = System.currentTimeMillis() - startTime;
        log.info("文档向量化并导入 Milvus 完成，耗时: {}ms", duration);

        // 5. 创建标记文件
        Files.createDirectories(markerPath.getParent());
        Files.createFile(markerPath);
        log.info("已创建标记文件: {}", markerPath.toAbsolutePath());

        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        // 配置检索器：最大返回 2 条结果，相似度阈值 0.6
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();
    }

    @Bean
    com.example.langchain4j.service.KnowledgeBaseService knowledgeBaseService(ChatLanguageModel chatLanguageModel,
            ContentRetriever contentRetriever) {
        return AiServices.builder(com.example.langchain4j.service.KnowledgeBaseService.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever) // 注入检索器，启用 RAG
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
