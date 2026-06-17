package com.yupi.aitravelplanner.rag;

// Lombok:自动生成 log 字段,免去手写 private static final Logger log = LoggerFactory.getLogger(...)
import lombok.extern.slf4j.Slf4j;

// Spring AI 文档抽象,一个 Document = 一段切好的文本 + 元数据(Map<String,Object>)
import org.springframework.ai.document.Document;

// Spring AI 内置的 Markdown 读取器
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;

// 读取器的配置(切片规则、保留哪些块、附加什么元数据)
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;

// Spring 资源抽象
import org.springframework.core.io.Resource;

// classpath:* 通配符解析器,能把 "classpath:document/*.md" 转成 Resource[]
import org.springframework.core.io.support.ResourcePatternResolver;

// 标成 Spring 组件,后续 TravelVectorStoreConfig 可以 @Autowired 注入
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 旅游攻略 Markdown 文档加载器
 *
 * 职责:把 classpath:document/ 目录下的所有 .md 文件切成 List<Document>
 * 切片规则:按 --- 横向分隔线切;代码块、引用块保留在原段落中
 * 元数据:每个 Document 携带 doc_type=文件名,后续可按 doc_type 过滤/回溯
 */
@Slf4j
@Component
public class TravelDocumentLoader {

    // 通配符路径常量——集中管理,后续要改目录(比如换成 classpath:travel/*.md)只改这里
    private static final String DOCUMENT_PATH = "classpath:document/*.md";

    // Spring 会自动注入这个 Bean(ResourcePatternResolver 是 spring-web 自带)
    // 它的作用:把 "classpath:document/*.md" 这种通配符路径解析成具体的 Resource[]
    private final ResourcePatternResolver resourcePatternResolver;

    // 构造注入——保证 resourcePatternResolver 不为 null,final 关键字禁止后续修改
    public TravelDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档
     *
     * @return 切分后的 Document 列表;若目录为空则返回空列表(不抛异常)
     */
    public List<Document> loadMarkdownDocuments() {
        // 累计所有 .md 文件切出来的 Document
        List<Document> allDocuments = new ArrayList<>();

        try {
            // 1) 解析通配符:把 classpath:document/*.md 转成 Resource[]
            //    这是方法里【唯一】可能抛 IOException 的地方
            Resource[] resources = resourcePatternResolver.getResources(DOCUMENT_PATH);

            // 2) 逐个 .md 文件处理
            for (Resource resource : resources) {

                // 取出文件名,例如 "travel-short-trip.md",作为 doc_type 元数据的值
                String filename = resource.getFilename();

                // 3) 配置本次读取的规则
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        // 开启按 --- 横向分隔线切分:一个 .md 切出多个 Document
                        .withHorizontalRuleCreateDocument(true)
                        // 保留 Markdown 中的 ```代码块``` 内容(不单独成 Document)
                        .withIncludeCodeBlock(true)
                        // 保留 Markdown 中的 > 引用块 内容(不单独成 Document)
                        .withIncludeBlockquote(true)
                        // 附加元数据 doc_type=文件名
                        // 后续可以在检索时按 doc_type 过滤,也能在答案里看到来源
                        .withAdditionalMetadata("doc_type", filename)
                        // 收尾,builder 模式必须 .build()
                        .build();

                // 4) 构造读取器
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);

                // 5) 执行读取,拿到这个 .md 切出来的所有 Document,累加到结果集
                //    reader.get() 不抛异常,不需要再 try/catch
                allDocuments.addAll(reader.get());
            }

            // 加载完成,记录日志(用 @Slf4j 生成的 log 字段)
            log.info("Loaded {} documents from {}", allDocuments.size(), DOCUMENT_PATH);

        } catch (IOException e) {
            // 读资源失败(类路径不对、目录不存在、权限问题等)时记录错误
            // 不抛出去——上层(RAG 配置)可以容忍空知识库,启动后由运营/开发补 .md
            log.error("Failed to load documents from {}", DOCUMENT_PATH, e);
        }

        return allDocuments;
    }
}
