package com.yupi.aitravelplanner.chatmemory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
@ActiveProfiles("test")
class FileBasedChatMemoryTest {

    @Test
    void shouldPersistMessagesAcrossInstances(@TempDir Path tempDir) {
        String conversationId = "test-conversation";
        String dir = tempDir.toString();

        FileBasedChatMemory memory1 = new FileBasedChatMemory(dir);
        memory1.add(conversationId, List.of(
                new UserMessage("我想去杭州玩3天"),
                new AssistantMessage("好的，杭州很适合短途旅行")
        ));

        FileBasedChatMemory memory2 = new FileBasedChatMemory(dir);
        List<Message> messages = memory2.get(conversationId);

        assertEquals(2, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
        assertTrue(messages.get(1) instanceof AssistantMessage);
    }

    @Test
    void shouldClearConversation(@TempDir Path tempDir) {
        String conversationId = "clear-test";
        String dir = tempDir.toString();

        FileBasedChatMemory memory = new FileBasedChatMemory(dir);
        memory.add(conversationId, List.of(new UserMessage("hello")));
        memory.clear(conversationId);

        assertTrue(memory.get(conversationId).isEmpty());
    }
}
