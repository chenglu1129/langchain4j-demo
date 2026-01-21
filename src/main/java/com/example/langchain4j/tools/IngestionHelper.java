package com.example.langchain4j.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 向量数据库导入助手
 * 用于处理增量文件导入，避免重复导入和全量重建
 */
@Component
@Slf4j
public class IngestionHelper {

    /**
     * 获取需要导入的新文件列表
     *
     * @param storeType     向量库类型标识 (例如 "pgvector", "milvus")
     * @param oldMarkerFile 旧的标记文件名 (用于兼容旧版本，例如 "data/.pgvector_ingested")
     * @return 需要导入的文件路径列表
     */
    public List<Path> resolveNewFiles(String storeType, String oldMarkerFile) {
        try {
            // 1. 获取 documents 目录下的所有文件
            List<Path> allFiles = getAllDocumentFiles();
            if (allFiles.isEmpty()) {
                log.warn("documents 目录下没有找到文件");
                return Collections.emptyList();
            }

            // 2. 确定新版清单文件路径
            Path inventoryPath = Paths.get("data", "." + storeType + "_inventory");

            // 3. 读取已导入的文件清单
            Set<String> ingestedFiles = new HashSet<>();
            if (Files.exists(inventoryPath)) {
                // 如果清单文件存在，读取其中的文件名
                ingestedFiles.addAll(Files.readAllLines(inventoryPath, StandardCharsets.UTF_8));
            } else {
                // 如果清单文件不存在，检查是否存在旧版标记文件 (兼容性处理)
                Path oldMarkerPath = Paths.get(oldMarkerFile);
                if (Files.exists(oldMarkerPath)) {
                    log.info("检测到旧版标记文件: {}，正在迁移至增量清单...", oldMarkerPath);
                    // 假设所有当前存在的文件都已导入（为了避免重复数据）
                    // 将当前所有文件写入新清单，并返回空列表（不进行导入）
                    updateInventory(inventoryPath, allFiles);
                    // 删除旧标记文件，完成迁移
                    Files.delete(oldMarkerPath);
                    log.info("迁移完成，旧标记文件已删除。下次添加新文件时将自动识别。");
                    return Collections.emptyList();
                }
            }

            // 4. 对比找出新增文件
            List<Path> newFiles = allFiles.stream()
                    .filter(file -> !ingestedFiles.contains(file.getFileName().toString()))
                    .collect(Collectors.toList());

            if (newFiles.isEmpty()) {
                log.info("[{}] 没有检测到新文档，跳过导入。", storeType);
            } else {
                log.info("[{}] 检测到 {} 个新文档: {}", storeType, newFiles.size(), 
                        newFiles.stream().map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")));
            }

            return newFiles;

        } catch (IOException | URISyntaxException e) {
            log.error("解析增量文件失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 更新清单文件，将新导入的文件名追加进去
     *
     * @param storeType 向量库类型标识
     * @param newFiles  新导入的文件列表
     */
    public void updateInventory(String storeType, List<Path> newFiles) {
        if (newFiles == null || newFiles.isEmpty()) {
            return;
        }
        Path inventoryPath = Paths.get("data", "." + storeType + "_inventory");
        updateInventory(inventoryPath, newFiles);
    }

    private void updateInventory(Path inventoryPath, List<Path> files) {
        try {
            if (!Files.exists(inventoryPath.getParent())) {
                Files.createDirectories(inventoryPath.getParent());
            }

            List<String> fileNames = files.stream()
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            Files.write(inventoryPath, fileNames, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            log.info("已更新导入清单: {}", inventoryPath);
        } catch (IOException e) {
            log.error("更新导入清单失败", e);
        }
    }

    private List<Path> getAllDocumentFiles() throws URISyntaxException, IOException {
        URL url = IngestionHelper.class.getClassLoader().getResource("documents");
        if (url == null) {
            return Collections.emptyList();
        }

        Path documentPath;
        try {
            documentPath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            documentPath = Paths.get(url.getPath());
        }

        try (Stream<Path> stream = Files.walk(documentPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().startsWith(".")) // 忽略隐藏文件
                    .collect(Collectors.toList());
        }
    }
}
