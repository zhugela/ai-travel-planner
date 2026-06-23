package com.yupi.aitravelplanner.constant;

/**
 * 文件路径常量
 */
public interface FileConstant {

    /**
     * 旅行规划 System Prompt 模板路径
     */
    String TRAVEL_SYSTEM_PROMPT_PATH = "prompts/travel-system.st";

    /**
     * 对话记忆持久化目录（相对项目根目录）
     */
    String CHAT_MEMORY_DIR = "tmp/chat-memory";

    /**
     * 文件操作工具隔离目录
     */
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp/file";

    /**
     * 资源下载工具隔离目录
     */
    String DOWNLOAD_SAVE_DIR = System.getProperty("user.dir") + "/tmp/download";

    /**
     * PDF 生成工具隔离目录
     */
    String PDF_SAVE_DIR = System.getProperty("user.dir") + "/tmp/pdf";

    /**
     * 终端操作工具允许执行的命令（白名单，逗号分隔）
     */
    String TERMINAL_ALLOWED_COMMANDS = "python3,python,node,npm,java,javac";
}
