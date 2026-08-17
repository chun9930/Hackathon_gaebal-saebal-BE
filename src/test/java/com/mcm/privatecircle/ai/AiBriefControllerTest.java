package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.ai.controller.AiBriefController;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AiBriefControllerTest {

    private final AiBriefService service = org.mockito.Mockito.mock(AiBriefService.class);
    private final AiBriefController controller = new AiBriefController(service);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void getLatestReturns200() {
        AiBriefResponse response = response(10L, 20L, 30L, BriefStatus.GENERATED);
        when(service.getLatest(caUser, 20L, 30L)).thenReturn(response);

        var result = controller.getLatest(caUser, 20L, 30L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getData()).isEqualTo(response);
        verify(service).getLatest(caUser, 20L, 30L);
    }

    @Test
    void getHistoryReturnsPageResponse() {
        PageResponse<AiBriefResponse> page = new PageResponse<>(
            List.of(response(10L, 20L, 30L, BriefStatus.FAILED)),
            0,
            20,
            1,
            1,
            false
        );
        when(service.getHistory(caUser, 20L, 0, 20)).thenReturn(page);

        var result = controller.getHistory(caUser, 20L, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getData()).isEqualTo(page);
        verify(service).getHistory(caUser, 20L, 0, 20);
    }

    private AiBriefResponse response(
        Long briefId,
        Long customerId,
        Long visitId,
        BriefStatus status
    ) {
        return new AiBriefResponse(
            briefId,
            customerId,
            visitId,
            2L,
            "summary",
            "visit purpose",
            "interest",
            "caution",
            "direction",
            3,
            status,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );
    }
}
