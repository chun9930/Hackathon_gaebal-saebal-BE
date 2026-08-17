package com.mcm.privatecircle.global.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;

import org.junit.jupiter.api.Test;

class PaginationValidatorTest {

    @Test
    void 허용된_페이지_범위를_통과시킨다() {
        assertThatCode(() -> PaginationValidator.validate(0, 1)).doesNotThrowAnyException();
        assertThatCode(() -> PaginationValidator.validate(3, 100)).doesNotThrowAnyException();
    }

    @Test
    void 음수_page를_INVALID_REQUEST로_거절한다() {
        assertInvalid(-1, 20);
    }

    @Test
    void 범위를_벗어난_size를_INVALID_REQUEST로_거절한다() {
        assertInvalid(0, 0);
        assertInvalid(0, 101);
    }

    private void assertInvalid(int page, int size) {
        assertThatThrownBy(() -> PaginationValidator.validate(page, size))
            .isInstanceOfSatisfying(BusinessException.class,
                exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
