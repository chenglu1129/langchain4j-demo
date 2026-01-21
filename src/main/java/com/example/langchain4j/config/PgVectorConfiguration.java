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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.example.langchain4j.tools.IngestionHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

@Configuration
@Profile("pgvector")
@Slf4j
@RequiredArgsConstructor
public class PgVectorConfiguration {

    private final AppProperties appProperties;
    private final IngestionHelper ingestionHelper;

    @Bean
    EmbeddingModel embeddingModel() {
        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    private static final String OLD_MARKER_FILE = "data/.pgvector_ingested";
    private static final String STORE_TYPE = "pgvector";

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) throws IOException, URISyntaxException {
        String table = appProperties.getVectorStore().getCollectionName();

        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host(appProperties.getVectorStore().getPgvector().getHost())
                .port(appProperties.getVectorStore().getPgvector().getPort())
                .database(appProperties.getVectorStore().getPgvector().getDatabase())
                .user(appProperties.getVectorStore().getPgvector().getUser())
                .password(appProperties.getVectorStore().getPgvector().getPassword())
                .table(table)
                .dimension(appProperties.getEmbedding().getDimension())
                .useIndex(true)
                .indexListSize(100)
                .createTable(true)
                .dropTableFirst(false)
                .build();

        // 1. 获取需要导入的新文件
        List<Path> newFiles = ingestionHelper.resolveNewFiles(STORE_TYPE, OLD_MARKER_FILE);

        if (newFiles.isEmpty()) {
            return embeddingStore;
        }

        // 2. 加载新文件
        long startTime = System.currentTimeMillis();
        List<Document> documents = newFiles.stream()
                .map(path -> FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser()))
                .toList();

        log.info("加载了 {} 个新文档", documents.size());

        // 3. 将文档切分并存入向量数据库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(
                        appProperties.getDocument().getMaxSegmentSize(),
                        appProperties.getDocument().getMaxOverlapSize()))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);

        long duration = System.currentTimeMillis() - startTime;
        log.info("新文档向量化并导入 PGVector 完成，耗时: {}ms", duration);

        // 4. 更新清单文件
        ingestionHelper.updateInventory(STORE_TYPE, newFiles);

        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(appProperties.getRetriever().getMaxResults())
                .minScore(appProperties.getRetriever().getMinScore())
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
