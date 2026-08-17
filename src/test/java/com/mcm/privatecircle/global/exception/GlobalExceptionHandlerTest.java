package com.mcm.privatecircle.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.mcm.privatecircle.global.response.ApiResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    @Test
    void 예상하지_못한_예외의_내부_메시지를_노출하지_않는다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Void>> response =
            handler.handleException(new IllegalStateException("DB 비밀번호가 포함된 내부 오류"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().code())
            .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        assertThat(response.getBody().getError().message())
            .isEqualTo("서버 내부 오류가 발생했습니다.")
            .doesNotContain("DB 비밀번호");
    }
}
