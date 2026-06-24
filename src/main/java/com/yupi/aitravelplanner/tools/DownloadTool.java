package com.yupi.aitravelplanner.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 资源下载工具
 * 下载网络资源到本地，统一隔离到 /tmp/download 目录
 */
@Component
@Slf4j
public class DownloadTool {

    private final String saveDir = FileConstant.DOWNLOAD_SAVE_DIR;

    /**
     * 下载网络资源到本地
     *
     * @param url 要下载的资源 URL
     * @return 成功返回本地文件路径，失败返回错误信息
     */
    @Tool(description = "下载网络资源到本地,参数为资源URL,成功返回本地文件路径")
    public String downloadResource(
            @ToolParam(description = "要下载的资源URL") String url
    ) {
        // URL 基本校验
        if (url == null || url.trim().isEmpty()) {
            return "错误: URL 不能为空";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "错误: URL 必须以 http:// 或 https:// 开头";
        }

        try {
            // 确保目录存在
            File dir = new File(saveDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return "创建下载目录失败";
                }
            }

            // 从 URL 提取文件名
            String fileName = extractFileName(url);
            File targetFile = new File(dir, fileName);

            log.info("开始下载: {} -> {}", url, targetFile.getAbsolutePath());

            // 下载文件
            HttpUtil.downloadFile(url, targetFile);

            // 检查文件是否下载成功
            if (!targetFile.exists() || targetFile.length() == 0) {
                return "下载失败: 文件为空";
            }

            return "下载成功，文件路径: " + targetFile.getAbsolutePath()
                    + " (大小: " + formatFileSize(targetFile.length()) + ")";

        } catch (Exception e) {
            log.error("下载失败: {}", url, e);
            return "下载失败: " + e.getMessage();
        }
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileName(String url) {
        try {
            // 去掉查询参数
            String path = url.split("\\?")[0];
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash > 0 && lastSlash < path.length() - 1) {
                String fileName = path.substring(lastSlash + 1);
                // 如果文件名有扩展名，直接返回
                if (fileName.contains(".")) {
                    return fileName;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        // 使用时间戳作为文件名
        return "download_" + System.currentTimeMillis();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        } else {
            return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
        }
    }

    /**
     * 获取下载工具的工作目录
     */
    @Tool(description = "获取资源下载工具的工作目录")
    public String getDownloadWorkDir() {
        return "资源下载工具工作目录: " + saveDir;
    }
}
