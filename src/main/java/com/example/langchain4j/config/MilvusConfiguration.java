package com.example.langchain4j.config;

import com.example.langchain4j.tools.IngestionHelper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

@Configuration
@Profile("milvus") // 仅在 milvus profile 激活时生效
@Slf4j
@RequiredArgsConstructor
public class MilvusConfiguration {

    private final AppProperties appProperties;
    private final IngestionHelper ingestionHelper;

    @Bean
    EmbeddingModel embeddingModel() {
        // 使用本地量化的 BGE-Small-ZH 模型，专门针对中文优化，且无需联网
        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    private static final String OLD_MARKER_FILE = "data/.milvus_ingested";
    private static final String STORE_TYPE = "milvus";

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) throws IOException, URISyntaxException {
        // 配置 Milvus 向量数据库连接
        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri(appProperties.getVectorStore().getMilvus().getUrl())
                .collectionName(appProperties.getVectorStore().getCollectionName())
                .dimension(appProperties.getEmbedding().getDimension()) // BGE-Small-ZH 模型的向量维度
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
        log.info("新文档向量化并导入 Milvus 完成，耗时: {}ms", duration);

        // 4. 更新清单文件
        ingestionHelper.updateInventory(STORE_TYPE, newFiles);

        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        // 配置检索器：最大返回 2 条结果，相似度阈值 0.6
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
                .contentRetriever(contentRetriever) // 注入检索器，启用 RAG
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
