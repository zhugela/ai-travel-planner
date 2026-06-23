package com.yupi.aitravelplanner.tools;

import cn.hutool.core.io.FileUtil;
import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件操作工具
 * 提供本地文本文件的读写功能，统一隔离到 /tmp/file 目录
 */
@Component
@Slf4j
public class FileOperationTool {

    private final String baseDir = FileConstant.FILE_SAVE_DIR;

    /**
     * 读取指定文件的文本内容
     *
     * @param fileName 文件名称，如 "北京攻略.md"
     * @return 文件内容，失败返回错误信息
     */
    @Tool(description = "读取指定文件的文本内容,参数为文件名,如'北京攻略.md'")
    public String readFile(
            @ToolParam(description = "文件名称,如'北京攻略.md'") String fileName
    ) {
        try {
            File dir = new File(baseDir);
            if (!dir.exists()) {
                return "文件目录不存在，请先写入文件";
            }

            File file = new File(dir, fileName);
            if (!file.exists()) {
                return "文件不存在: " + fileName;
            }

            String content = FileUtil.readUtf8String(file);
            return content;
        } catch (Exception e) {
            log.error("读取文件失败: {}", fileName, e);
            return "读取文件失败: " + e.getMessage();
        }
    }

    /**
     * 把文本内容写入到指定文件
     *
     * @param fileName 文件名称，如 "东京之旅.md"
     * @param content  待写入的文本内容
     * @return 成功返回文件路径，失败返回错误信息
     */
    @Tool(description = "把文本内容写入到指定文件,成功返回文件路径")
    public String writeFile(
            @ToolParam(description = "文件名称,如'东京之旅.md'") String fileName,
            @ToolParam(description = "待写入的文本内容") String content
    ) {
        try {
            File dir = new File(baseDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return "创建目录失败";
                }
            }

            File file = new File(dir, fileName);
            FileUtil.writeUtf8String(content, file);
            return "文件已保存到: " + file.getAbsolutePath();
        } catch (Exception e) {
            log.error("写入文件失败: {}", fileName, e);
            return "文件保存失败: " + e.getMessage();
        }
    }

    /**
     * 获取文件操作工具的工作目录
     *
     * @return 工作目录路径
     */
    @Tool(description = "获取文件操作工具的工作目录路径")
    public String getWorkDir() {
        return "文件操作工具工作目录: " + baseDir;
    }
}
