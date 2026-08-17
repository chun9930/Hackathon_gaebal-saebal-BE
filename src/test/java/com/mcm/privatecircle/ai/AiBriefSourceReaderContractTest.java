package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;

class AiBriefSourceReaderContractTest {

    @Test
    void Source_조회기는_Principal과_고객과_기준_방문을_입력으로_받는다() throws Exception {
        Class<?> readerType = Class.forName(
            "com.mcm.privatecircle.ai.service.AiBriefSourceReader"
        );

        assertThat(readerType.getMethod(
            "read",
            AuthenticatedUser.class,
            Long.class,
            Long.class
        ).getReturnType().getName()).isEqualTo(
            "com.mcm.privatecircle.ai.dto.AiBriefSource"
        );
    }
}
