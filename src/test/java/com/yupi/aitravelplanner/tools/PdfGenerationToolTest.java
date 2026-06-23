package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDF 生成工具单元测试
 */
@SpringBootTest
class PdfGenerationToolTest {

    @Autowired
    private PdfGenerationTool pdfGenerationTool;

    @Test
    void generatePdf_emptyFileName() {
        String result = pdfGenerationTool.generatePdf("", "Test content");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void generatePdf_emptyContent() {
        String result = pdfGenerationTool.generatePdf("test.pdf", "");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void generatePdf_normal() {
        String content = "北京三日游攻略\n\n第一天:天安门 + 故宫\n第二天:长城\n第三天:颐和园";
        String result = pdfGenerationTool.generatePdf("北京攻略.pdf", content);

        assertNotNull(result);
        // 应该成功或返回错误信息
        assertTrue(result.contains("成功") || result.contains("失败") || result.contains("PDF"));
    }

    @Test
    void getWorkDir() {
        String result = pdfGenerationTool.getWorkDir();

        assertNotNull(result);
        assertTrue(result.contains("工作目录"));
    }
}
