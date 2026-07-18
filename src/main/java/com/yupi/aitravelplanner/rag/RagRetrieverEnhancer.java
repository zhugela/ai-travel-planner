package com.yupi.aitravelplanner.rag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索质量优化组件(Ch 05 §三)。
 *
 * 提供两个能力:
 *   1) Re-ranking:对 Spring AI 检索出的文档按 query 关键词做二次排序,
 *                  解决"向量相似度高但关键词不匹配"的问题(笔记 §三.2)
 *   2) Query 缓存:同一个 query 的检索结果缓存 10 分钟(笔记 §五.3),
 *                  避免重复 embedding 同样 query 浪费 token
 *
 * 设计要点:
 *   - 不依赖任何外部库,纯 JDK + Caffeine
 *   - Re-ranking 用词袋 + Jaccard 相似度,简单可解释
 *   - 缓存 key = query 字符串,value = 文档列表
 */
@Slf4j
@Component
public class RagRetrieverEnhancer {

    /** Query 缓存:同一 query 10 分钟内复用 */
    private final Cache<String, List<Document>> queryCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    /**
     * Re-rank 文档列表:按 query 与文档内容的词袋 Jaccard 相似度降序排序。
     *
     * @param query 用户原始查询
     * @param docs Spring AI 检索器返回的候选文档
     * @param topK 取前 K 条
     * @return 排序后的文档(相关性更高的在前)
     */
    public List<Document> rerank(String query, List<Document> docs, int topK) {
        if (docs == null || docs.isEmpty()) return List.of();
        if (query == null || query.isBlank()) return docs;

        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return docs;

        // 每条文档算一个分:与 query 的 Jaccard 相似度
        Map<Document, Double> scoreMap = new HashMap<>();
        for (Document doc : docs) {
            Set<String> docTokens = tokenize(doc.getText());
            double jaccard = jaccardSimilarity(queryTokens, docTokens);
            // 综合分 = Jaccard 相似度,范围 0~1
            scoreMap.put(doc, jaccard);
        }

        // 按分数降序排序,取前 K
        List<Document> reranked = docs.stream()
                .sorted((a, b) -> Double.compare(scoreMap.get(b), scoreMap.get(a)))
                .limit(topK)
                .collect(Collectors.toList());

        log.debug("[Re-rank] query='{}' 输入 {} 条 → 输出 {} 条,最高分={}",
                query, docs.size(), reranked.size(),
                reranked.isEmpty() ? 0 : scoreMap.get(reranked.get(0)));
        return reranked;
    }

    /**
     * 带缓存的检索增强:如果 query 已在缓存中,直接返回;
     * 否则调 retriever 检索 → re-rank → 缓存 → 返回。
     *
     * @param query 用户查询
     * @param retriever 实际的检索回调(Spring AI 的 vectorStore.similaritySearch)
     * @param topK 返回前 K 条
     * @return 排序后的文档列表
     */
    public List<Document> retrieveWithCache(String query,
                                             java.util.function.Function<String, List<Document>> retriever,
                                             int topK) {
        // 1. 先查缓存
        List<Document> cached = queryCache.getIfPresent(query);
        if (cached != null) {
            log.debug("[Cache HIT] query='{}' 命中缓存,返回 {} 条", query, cached.size());
            return cached;
        }

        // 2. 缓存未命中 → 实际检索
        List<Document> raw = retriever.apply(query);
        if (raw == null) raw = List.of();

        // 3. Re-rank
        List<Document> reranked = rerank(query, raw, topK);

        // 4. 写回缓存
        queryCache.put(query, reranked);
        log.debug("[Cache MISS] query='{}' 检索 {} 条 → rerank 后 {} 条,已缓存",
                query, raw.size(), reranked.size());
        return reranked;
    }

    /** 清空缓存(测试用 / 手动刷新知识库时用) */
    public void clearCache() {
        queryCache.invalidateAll();
        log.info("[Cache] 已清空所有 query 缓存");
    }

    /** 当前缓存大小(监控用) */
    public long cacheSize() {
        return queryCache.estimatedSize();
    }

    // ===== 私有工具方法 =====

    /** 中文 + 英文混合分词:按字符切分 + 转小写 + 去停用词 */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();

        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}]", " "); // 去标点

        Set<String> stopWords = Set.of(
                "的", "了", "是", "在", "和", "与", "或", "及", "我", "你", "他", "她",
                "我们", "你们", "他们", "这", "那", "一个", "一些", "什么", "怎么", "如何",
                "the", "a", "an", "is", "are", "was", "were", "in", "on", "at", "to",
                "for", "of", "and", "or", "but", "is", "i", "you", "we", "they", "this", "that"
        );

        Set<String> tokens = new HashSet<>();
        // 中文按 2-gram 切(粗粒度,够用);英文按单词切
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            if (word.isBlank() || stopWords.contains(word)) continue;
            // 中文 2-gram
            if (word.matches("[\\u4e00-\\u9fa5]+") && word.length() >= 2) {
                for (int i = 0; i < word.length() - 1; i++) {
                    String gram = word.substring(i, i + 2);
                    if (!stopWords.contains(gram)) tokens.add(gram);
                }
            } else if (word.length() >= 2) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /** Jaccard 相似度 = |A ∩ B| / |A ∪ B| */
    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}