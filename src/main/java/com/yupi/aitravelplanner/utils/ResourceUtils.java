package com.yupi.aitravelplanner.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 资源文件读取工具
 */
public class ResourceUtils {

    private ResourceUtils() {
    }

    public static String readClasspathFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取资源文件失败: " + path, e);
        }
    }
}
