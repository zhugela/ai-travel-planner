package com.yupi.aitravelplanner.common;

import com.yupi.aitravelplanner.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultUtils 单元测试 —— 快速构造响应（纯逻辑，不依赖 Spring）
 */
@DisplayName("ResultUtils 响应构造")
class ResultUtilsTest {

    // ======================== success ========================

    @Test
    @DisplayName("success(data) → code=0, message=ok, data 回显")
    void successWithData() {
        BaseResponse<String> resp = ResultUtils.success("北京攻略");

        assertThat(resp.getCode()).isEqualTo(0);
        assertThat(resp.getMessage()).isEqualTo("ok");
        assertThat(resp.getData()).isEqualTo("北京攻略");
    }

    @Test
    @DisplayName("success() → code=0, message=ok, data=null")
    void successWithoutData() {
        BaseResponse<?> resp = ResultUtils.success();

        assertThat(resp.getCode()).isEqualTo(0);
        assertThat(resp.getMessage()).isEqualTo("ok");
        assertThat(resp.getData()).isNull();
    }

    // ======================== error(ErrorCode) ========================

    @Test
    @DisplayName("error(ErrorCode) → code/message 来自枚举")
    void errorFromErrorCode() {
        BaseResponse<?> resp = ResultUtils.error(ErrorCode.PARAMS_ERROR);

        assertThat(resp.getCode()).isEqualTo(40000);
        assertThat(resp.getMessage()).isEqualTo("请求参数错误");
        assertThat(resp.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode.SYSTEM_ERROR) → code=50000")
    void errorSystemError() {
        BaseResponse<?> resp = ResultUtils.error(ErrorCode.SYSTEM_ERROR);

        assertThat(resp.getCode()).isEqualTo(50000);
        assertThat(resp.getMessage()).isEqualTo("系统内部异常");
    }

    // ======================== error(int, String) ========================

    @Test
    @DisplayName("error(code, message) → 自定义 code 和 message")
    void errorCustomCodeAndMessage() {
        BaseResponse<?> resp = ResultUtils.error(99999, "自定义错误");

        assertThat(resp.getCode()).isEqualTo(99999);
        assertThat(resp.getMessage()).isEqualTo("自定义错误");
        assertThat(resp.getData()).isNull();
    }

    // ======================== error(ErrorCode, String) ========================

    @Test
    @DisplayName("error(ErrorCode, message) → code 来自枚举，message 自定义")
    void errorCodeWithCustomMessage() {
        BaseResponse<?> resp = ResultUtils.error(ErrorCode.PARAMS_ERROR, "用户名不能为空");

        assertThat(resp.getCode()).isEqualTo(40000);
        assertThat(resp.getMessage()).isEqualTo("用户名不能为空");
    }

    // ======================== 边界 ========================

    @Test
    @DisplayName("success(null) → code=0, message=ok, data=null")
    void successWithNullData() {
        BaseResponse<Object> resp = ResultUtils.success(null);

        assertThat(resp.getCode()).isEqualTo(0);
        assertThat(resp.getMessage()).isEqualTo("ok");
        assertThat(resp.getData()).isNull();
    }
}
