package com.yupi.aitravelplanner.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RagRetrieverEnhancer 单元测试 —— P2：Re-rank + 缓存（纯逻辑，不依赖 Spring / AI API）
 */
@DisplayName("RagRetrieverEnhancer 检索增强")
class RagRetrieverEnhancerTest {

    private RagRetrieverEnhancer enhancer;

    @BeforeEach
    void setUp() {
        enhancer = new RagRetrieverEnhancer();
        enhancer.clearCache();
    }

    // ======================== Re-rank ========================

    @Test
    @DisplayName("Re-rank: 相关文档排在前面")
    void rerank_relevantDocFirst() {
        List<Document> docs = List.of(
                new Document("Python 是一门编程语言"),
                new Document("北京故宫是中国著名的旅游景点"),
                new Document("机器学习是人工智能的一个分支")
        );

        List<Document> result = enhancer.rerank("北京故宫旅游", docs, 3);

        assertThat(result).hasSize(3);
        // 最相关的"北京故宫"文档应该排第一
        assertThat(result.get(0).getText()).contains("故宫");
    }

    @Test
    @DisplayName("Re-rank: 空列表 → 返回空")
    void rerank_emptyList_returnsEmpty() {
        List<Document> result = enhancer.rerank("北京", List.of(), 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Re-rank: null → 返回空")
    void rerank_nullDocs_returnsEmpty() {
        List<Document> result = enhancer.rerank("北京", null, 5);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Re-rank: 空 query → 返回原列表")
    void rerank_emptyQuery_returnsOriginal() {
        List<Document> docs = List.of(new Document("测试内容"));
        List<Document> result = enhancer.rerank("", docs, 5);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Re-rank: topK 限制生效")
    void rerank_topKLimitsResults() {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            docs.add(new Document("文档内容 " + i));
        }

        List<Document> result = enhancer.rerank("文档 内容", docs, 3);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Re-rank: 与 query 关键词高度匹配的分数最高")
    void rerank_keywordMatch_scoresHighest() {
        List<Document> docs = List.of(
                new Document("苹果是一种美味的水果"),
                new Document("杭州西湖是著名的旅游景点"),
                new Document("今天天气很好")
        );

        List<Document> result = enhancer.rerank("杭州西湖旅游攻略", docs, 3);
        assertThat(result.get(0).getText()).contains("西湖");
    }

    // ======================== Query 缓存 ========================

    @Test
    @DisplayName("缓存: 同一 query 只触发 1 次检索（verify callback 次数）")
    void retrieveWithCache_sameQuery_hitsCache() {
        var callCount = new int[]{0};

        // 第一次：缓存 MISS
        List<Document> docs1 = enhancer.retrieveWithCache("杭州攻略", query -> {
            callCount[0]++;
            return List.of(new Document("杭州攻略文档"));
        }, 5);

        assertThat(docs1).hasSize(1);
        assertThat(callCount[0]).isEqualTo(1);

        // 第二次：缓存 HIT
        List<Document> docs2 = enhancer.retrieveWithCache("杭州攻略", query -> {
            callCount[0]++;
            return List.of(new Document("不应被调用"));
        }, 5);

        assertThat(docs2).hasSize(1);
        assertThat(callCount[0]).isEqualTo(1);  // 仍为 1，走缓存
    }

    @Test
    @DisplayName("缓存: 不同 query 各自触发检索")
    void retrieveWithCache_differentQuery_separateCache() {
        var callCount = new int[]{0};

        enhancer.retrieveWithCache("北京攻略", query -> {
            callCount[0]++;
            return List.of(new Document("北京"));
        }, 5);

        enhancer.retrieveWithCache("上海攻略", query -> {
            callCount[0]++;
            return List.of(new Document("上海"));
        }, 5);

        assertThat(callCount[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("缓存: clearCache() 后重新检索")
    void clearCache_resetsCache() {
        var callCount = new int[]{0};

        enhancer.retrieveWithCache("杭州攻略", query -> {
            callCount[0]++;
            return List.of(new Document("杭州攻略"));
        }, 5);
        assertThat(callCount[0]).isEqualTo(1);

        enhancer.clearCache();

        enhancer.retrieveWithCache("杭州攻略", query -> {
            callCount[0]++;
            return List.of(new Document("杭州攻略重新检索"));
        }, 5);
        assertThat(callCount[0]).isEqualTo(2);  // 缓存已清，再次检索
    }

    @Test
    @DisplayName("缓存: cacheSize() 返回正确条目数")
    void cacheSize_countsCorrectly() {
        enhancer.clearCache();
        assertThat(enhancer.cacheSize()).isEqualTo(0);

        enhancer.retrieveWithCache("query1", q -> List.of(new Document("a")), 5);
        assertThat(enhancer.cacheSize()).isEqualTo(1);

        enhancer.retrieveWithCache("query2", q -> List.of(new Document("b")), 5);
        assertThat(enhancer.cacheSize()).isEqualTo(2);
    }
}
