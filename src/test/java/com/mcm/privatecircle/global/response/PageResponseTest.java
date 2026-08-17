package com.mcm.privatecircle.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void Spring_Page를_공통_페이지_응답으로_변환한다() {
        PageImpl<String> page = new PageImpl<>(
            List.of("첫째", "둘째"),
            PageRequest.of(1, 2),
            5
        );

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.items()).containsExactly("첫째", "둘째");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }
}
