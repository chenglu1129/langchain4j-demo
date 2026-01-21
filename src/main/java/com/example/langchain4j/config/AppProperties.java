package com.example.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * 嵌入向量配置
     */
    private Embedding embedding = new Embedding();

    /**
     * 文档处理配置
     */
    private Document document = new Document();

    /**
     * 检索器配置
     */
    private Retriever retriever = new Retriever();

    /**
     * 向量数据库通用配置
     */
    private VectorStore vectorStore = new VectorStore();

    @Data
    public static class Embedding {
        /**
         * 向量维度 (BGE-Small-ZH 为 512)
         */
        private int dimension = 512;
    }

    @Data
    public static class Document {
        /**
         * 文档切分：最大片段长度
         */
        private int maxSegmentSize = 300;

        /**
         * 文档切分：重叠长度
         */
        private int maxOverlapSize = 0;
    }

    @Data
    public static class Retriever {
        /**
         * 最大返回结果数
         */
        private int maxResults = 2;

        /**
         * 最小相似度分数
         */
        private double minScore = 0.6;
    }

    @Data
    public static class VectorStore {
        /**
         * 索引/集合/表名称
         */
        private String collectionName = "langchain4j_vectors";

        /**
         * Chroma 配置
         */
        private Chroma chroma = new Chroma();

        /**
         * Elasticsearch 配置
         */
        private Elasticsearch elasticsearch = new Elasticsearch();

        /**
         * Milvus 配置
         */
        private Milvus milvus = new Milvus();

        /**
         * PgVector 配置
         */
        private PgVector pgvector = new PgVector();

        @Data
        public static class Chroma {
            /**
             * Chroma 服务地址
             */
            private String url = "http://localhost:8000";
        }

        @Data
        public static class Elasticsearch {
            /**
             * Elasticsearch 服务地址
             */
            private String url = "http://localhost:9200";
        }

        @Data
        public static class Milvus {
            /**
             * Milvus 服务地址
             */
            private String url = "http://localhost:19530";
        }

        @Data
        public static class PgVector {
            /**
             * 主机地址
             */
            private String host = "localhost";

            /**
             * 端口号
             */
            private int port = 5432;

            /**
             * 数据库名称
             */
            private String database = "postgres";

            /**
             * 用户名
             */
            private String user = "postgres";

            /**
             * 密码
             */
            private String password = "postgres";
        }
    }
}
