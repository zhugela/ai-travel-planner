package com.yupi.aitravelplanner.tools;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

/**
 * PDF 生成工具
 * 使用 iText 生成 PDF 文件，支持中文字体，统一隔离到 /tmp/pdf 目录
 */
@Component
@Slf4j
public class PdfGenerationTool {

    private final String saveDir = FileConstant.PDF_SAVE_DIR;

    /**
     * 把文本内容生成为 PDF 文件
     *
     * @param fileName PDF 文件名，如 "北京攻略.pdf"
     * @param content  PDF 内容文本
     * @return 成功返回 PDF 文件路径，失败返回错误信息
     */
    @Tool(returnDirect = true, description = "把文本内容生成为PDF文件,返回PDF文件路径")
    public String generatePdf(
            @ToolParam(description = "PDF文件名,如'北京攻略.pdf'") String fileName,
            @ToolParam(description = "PDF内容文本") String content
    ) {
        // 参数校验
        if (fileName == null || fileName.trim().isEmpty()) {
            return "错误: 文件名不能为空";
        }

        // 确保文件名以 .pdf 结尾
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }

        if (content == null || content.trim().isEmpty()) {
            return "错误: 内容不能为空";
        }

        try {
            // 确保目录存在
            File dir = new File(saveDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return "创建 PDF 目录失败";
                }
            }

            File pdfFile = new File(dir, fileName);

            log.info("生成 PDF: {}", pdfFile.getAbsolutePath());

            // 生成 PDF
            try (FileOutputStream fos = new FileOutputStream(pdfFile);
                 PdfWriter writer = new PdfWriter(fos);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 中文字体支持（使用系统黑体字体）
                String fontPath = getChineseFontPath();
                PdfFont font = PdfFontFactory.createFont(fontPath);
                document.setFont(font);

                // 按换行符分段写入
                String[] paragraphs = content.split("\n");
                for (String para : paragraphs) {
                    if (!para.trim().isEmpty()) {
                        document.add(new Paragraph(para.trim()));
                    }
                }
            }

            return "PDF 生成成功: " + pdfFile.getAbsolutePath();

        } catch (Exception e) {
            log.error("PDF 生成失败: {}", fileName, e);
            return "PDF 生成失败: " + e.getMessage();
        }
    }

    /**
     * 获取中文字体路径（根据操作系统）
     */
    private String getChineseFontPath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows 系统使用黑体
            return "C:/Windows/Fonts/simhei.ttf";
        } else if (os.contains("mac")) {
            // macOS 系统
            return "/System/Library/Fonts/STHeiti Light.ttc";
        } else {
            // Linux 系统（需要安装中文字体）
            String[] linuxFonts = {
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
                    "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
            };

            for (String font : linuxFonts) {
                if (new File(font).exists()) {
                    return font;
                }
            }

            // 默认返回 Windows 路径
            return "C:/Windows/Fonts/simhei.ttf";
        }
    }

    /**
     * 获取 PDF 工具的工作目录
     */
    @Tool(description = "获取PDF生成工具的工作目录")
    public String getWorkDir() {
        return "PDF 生成工具工作目录: " + saveDir;
    }
}
