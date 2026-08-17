package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.mcm.privatecircle.ai.controller.AiBriefController;
import com.mcm.privatecircle.ai.dto.AiBriefCreateRequest;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AiBriefCreateControllerTest {

    private final AiBriefService service = org.mockito.Mockito.mock(AiBriefService.class);
    private final AiBriefController controller = new AiBriefController(service);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void createReturns201() {
        AiBriefCreateRequest request = new AiBriefCreateRequest(30L);
        AiBriefResponse response = new AiBriefResponse(
            10L,
            20L,
            30L,
            2L,
            "summary",
            "purpose",
            "interest",
            "caution",
            "direction",
            3,
            BriefStatus.GENERATED,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );
        when(service.create(caUser, 20L, request)).thenReturn(response);

        var result = controller.create(caUser, 20L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getData()).isEqualTo(response);
    }
}
