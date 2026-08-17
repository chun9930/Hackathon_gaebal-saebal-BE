package com.mcm.privatecircle.stamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.stamp.controller.VisitStampController;
import com.mcm.privatecircle.stamp.dto.VisitStampCreateRequest;
import com.mcm.privatecircle.stamp.dto.VisitStampResponse;
import com.mcm.privatecircle.stamp.service.VisitStampService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VisitStampControllerTest {

    private final VisitStampService service = mock(VisitStampService.class);
    private final VisitStampController controller = new VisitStampController(service);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);
    private final AuthenticatedUser customerUser = AuthenticatedUser.customer(4L, 5L);

    @Test
    void issueStampReturns201() {
        VisitStampCreateRequest request = new VisitStampCreateRequest("VISIT");
        VisitStampResponse result = mock(VisitStampResponse.class);
        when(service.issueStamp(caUser, 10L, request)).thenReturn(result);

        assertThat(controller.issueStamp(caUser, 10L, request).getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getStampListsReturn200AndPageResponse() {
        PageResponse<VisitStampResponse> page = new PageResponse<>(
            List.of(new VisitStampResponse(
                1L, 10L, 5L, "Alice", 3L, "Main Store", 2L, "Advisor Kim", "VISIT",
                LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 12, 0)
            )),
            0, 20, 1, 1, false
        );
        when(service.getCustomerStamps(caUser, 5L, 0, 20)).thenReturn(page);
        when(service.getMyStamps(customerUser, 0, 20)).thenReturn(page);

        assertThat(controller.getCustomerStamps(caUser, 5L, 0, 20).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(controller.getMyStamps(customerUser, 0, 20).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }
}