package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件操作工具单元测试
 */
@SpringBootTest
class FileOperationToolTest {

    @Autowired
    private FileOperationTool fileOperationTool;

    @TempDir
    Path tempDir;

    @Test
    void writeFile() {
        String fileName = "test-write.txt";
        String content = "Hello World! Test content.";

        String result = fileOperationTool.writeFile(fileName, content);

        assertNotNull(result);
        assertTrue(result.contains("已保存到"));
        assertTrue(result.contains(fileName));
    }

    @Test
    void readFile() {
        // 先写入
        String fileName = "test-read.txt";
        String content = "Test read content";
        fileOperationTool.writeFile(fileName, content);

        // 再读取
        String result = fileOperationTool.readFile(fileName);

        assertNotNull(result);
        assertEquals(content, result);
    }

    @Test
    void readFile_notExists() {
        String result = fileOperationTool.readFile("non-existent-file.txt");

        assertNotNull(result);
        assertTrue(result.contains("不存在") || result.contains("失败"));
    }

    @Test
    void getFileOpWorkDir() {
        String result = fileOperationTool.getFileOpWorkDir();

        assertNotNull(result);
        assertTrue(result.contains("工作目录"));
    }
}
