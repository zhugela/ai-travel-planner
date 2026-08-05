package com.yupi.aitravelplanner.exception;

import com.yupi.aitravelplanner.common.BaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler 单元测试 —— 异常兜底（纯 Mockito，不依赖 Spring MVC）
 * <p>
 * 直接调 handler 方法验证逻辑，不用 MockMvc。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 异常处理")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    // ======================== BusinessException ========================

    @Test
    @DisplayName("BusinessException → 回显业务 code 和 message")
    void businessException_returnsBusinessCode() {
        BusinessException ex = new BusinessException(40000, "目的地不能为空");

        BaseResponse<?> resp = handler.businessExceptionHandler(ex);

        assertThat(resp.getCode()).isEqualTo(40000);
        assertThat(resp.getMessage()).isEqualTo("目的地不能为空");
    }

    @Test
    @DisplayName("BusinessException(ErrorCode) → 回显枚举 code 和 message")
    void businessExceptionFromErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND_ERROR);

        BaseResponse<?> resp = handler.businessExceptionHandler(ex);

        assertThat(resp.getCode()).isEqualTo(40400);
        assertThat(resp.getMessage()).isEqualTo("请求数据不存在");
    }

    // ======================== RuntimeException ========================

    @Test
    @DisplayName("RuntimeException → 统一返回 SYSTEM_ERROR，不暴露内部消息")
    void runtimeException_returnsSystemError() {
        RuntimeException ex = new RuntimeException("数据库连接超时");

        BaseResponse<?> resp = handler.runtimeExceptionHandler(ex);

        assertThat(resp.getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        assertThat(resp.getMessage()).isEqualTo("系统错误");
        // 关键：不暴露原始异常消息到响应体
        assertThat(resp.getMessage()).isNotEqualTo("数据库连接超时");
    }

    @Test
    @DisplayName("RuntimeException(NPE) → 不暴露 null pointer 细节")
    void runtimeException_npe_hidesDetails() {
        RuntimeException ex = new NullPointerException();

        BaseResponse<?> resp = handler.runtimeExceptionHandler(ex);

        assertThat(resp.getCode()).isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        assertThat(resp.getMessage()).isEqualTo("系统错误");
    }
}
