package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.controller.VisitController;
import com.mcm.privatecircle.visit.dto.VisitCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitResponse;
import com.mcm.privatecircle.visit.service.VisitService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VisitControllerTest {

    private final VisitService visitService = org.mockito.Mockito.mock(VisitService.class);
    private final VisitController controller = new VisitController(visitService);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void createVisitReturns201() {
        VisitCreateRequest request = new VisitCreateRequest(
            10L,
            LocalDateTime.of(2026, 8, 17, 14, 0)
        );
        VisitResponse visit = new VisitResponse(20L, 10L, "Alice", 3L, "Main Store", request.visitedAt());
        when(visitService.createVisit(caUser, request)).thenReturn(visit);

        var response = controller.createVisit(caUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData()).isEqualTo(visit);
    }

    @Test
    void getCustomerVisitsReturnsPageResponse() {
        PageResponse<VisitResponse> page = new PageResponse<>(
            List.of(new VisitResponse(20L, 10L, "Alice", 3L, "Main Store", LocalDateTime.of(2026, 8, 17, 14, 0))),
            0,
            20,
            1,
            1,
            false
        );
        when(visitService.getCustomerVisits(caUser, 10L, 0, 20)).thenReturn(page);

        var response = controller.getCustomerVisits(caUser, 10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(page);
        verify(visitService).getCustomerVisits(caUser, 10L, 0, 20);
    }
}