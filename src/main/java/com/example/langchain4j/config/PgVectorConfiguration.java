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
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
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
@Profile("pgvector")
@Slf4j
public class PgVectorConfiguration {

    @Bean
    EmbeddingModel embeddingModel() {
        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    private static final String INGESTION_MARKER_FILE = "data/.pgvector_ingested";

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) throws IOException, URISyntaxException {
        String host = System.getenv().getOrDefault("PGVECTOR_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("PGVECTOR_PORT", "5432"));
        String database = System.getenv().getOrDefault("PGVECTOR_DB", "postgres");
        String user = System.getenv().getOrDefault("PGVECTOR_USER", "postgres");
        String password = System.getenv().getOrDefault("PGVECTOR_PASSWORD", "postgres");
        String table = System.getenv().getOrDefault("PGVECTOR_TABLE", "langchain4j_embeddings");

        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(embeddingModel.dimension())
                .useIndex(true)
                .indexListSize(100)
                .createTable(true)
                .dropTableFirst(false)
                .build();

        Path markerPath = Paths.get(INGESTION_MARKER_FILE);
        if (Files.exists(markerPath)) {
            log.info("检测到标记文件: {}", markerPath.toAbsolutePath());
            log.info("假设 PGVector 已包含向量数据，跳过导入。");
            return embeddingStore;
        }

        log.info("未找到标记文件，准备导入数据到 PGVector...");

        URL url = PgVectorConfiguration.class.getClassLoader().getResource("documents");
        if (url == null) {
            log.warn("未找到 documents 目录，跳过知识库加载");
            return embeddingStore;
        }

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

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);

        long duration = System.currentTimeMillis() - startTime;
        log.info("文档向量化并导入 PGVector 完成，耗时: {}ms", duration);

        Files.createDirectories(markerPath.getParent());
        Files.createFile(markerPath);
        log.info("已创建标记文件: {}", markerPath.toAbsolutePath());

        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
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
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
