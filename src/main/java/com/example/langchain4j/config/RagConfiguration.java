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
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class RagConfiguration {

    @Bean
    EmbeddingModel embeddingModel() {
        // 使用本地量化的 BGE-Small-ZH 模型，专门针对中文优化，且无需联网
        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    // 向量数据持久化文件路径
    private static final String EMBEDDING_STORE_FILE = "data/embedding-store.json";

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) throws IOException, URISyntaxException {
        Path storePath = Paths.get(EMBEDDING_STORE_FILE);

        // 1. 检查是否存在已持久化的向量数据
        if (java.nio.file.Files.exists(storePath)) {
            System.out.println("从文件加载已有的向量数据: " + storePath.toAbsolutePath());
            long startTime = System.currentTimeMillis();

            InMemoryEmbeddingStore<TextSegment> embeddingStore = InMemoryEmbeddingStore.fromFile(storePath);

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("向量数据加载完成，耗时: " + duration + "ms");
            return embeddingStore;
        }

        // 2. 如果没有持久化数据，创建新的向量存储
        System.out.println("未找到已有的向量数据，开始新建...");
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 3. 加载文档
        URL url = RagConfiguration.class.getClassLoader().getResource("documents");
        if (url == null) {
            System.out.println("未找到 documents 目录，跳过知识库加载");
            return embeddingStore;
        }

        // 处理 Windows 路径问题
        Path documentPath;
        try {
            documentPath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            documentPath = Paths.get(url.getPath());
        }

        System.out.println("正在从以下路径加载文档: " + documentPath);
        long startTime = System.currentTimeMillis();

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                documentPath,
                new TextDocumentParser());

        System.out.println("加载了 " + documents.size() + " 个文档");

        // 4. 将文档切分并存入向量数据库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("文档向量化完成，耗时: " + duration + "ms");

        // 5. 持久化向量数据到文件
        java.nio.file.Files.createDirectories(storePath.getParent());
        embeddingStore.serializeToFile(storePath);
        System.out.println("向量数据已持久化到: " + storePath.toAbsolutePath());

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

    // 定义一个支持 RAG 的 AI 服务接口
    public interface KnowledgeBaseService {
        String chat(String userMessage);
    }

    @Bean
    KnowledgeBaseService knowledgeBaseService(ChatLanguageModel chatLanguageModel, ContentRetriever contentRetriever) {
        return AiServices.builder(KnowledgeBaseService.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever) // 注入检索器，启用 RAG
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
