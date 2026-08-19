package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.controller.VisitRecordController;
import com.mcm.privatecircle.visit.dto.VisitRecordCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitRecordResponse;
import com.mcm.privatecircle.visit.dto.VisitRecordUpdateRequest;
import com.mcm.privatecircle.visit.service.VisitRecordService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VisitRecordControllerTest {

    private final VisitRecordService service = org.mockito.Mockito.mock(VisitRecordService.class);
    private final VisitRecordController controller = new VisitRecordController(service);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void createVisitRecordReturns201() {
        VisitRecordCreateRequest request = new VisitRecordCreateRequest("purpose", "content", null, null);
        VisitRecordResponse result = new VisitRecordResponse(
            10L, 20L, 30L, "Alice", 2L, "Advisor Kim", 3L, "Main Store",
            LocalDateTime.of(2026, 8, 17, 14, 0), "purpose", "content", null, null, null
        );
        when(service.createVisitRecord(caUser, 20L, request)).thenReturn(result);

        var response = controller.createVisitRecord(caUser, 20L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData()).isEqualTo(result);
    }

    @Test
    void updateVisitRecordReturns200() {
        VisitRecordUpdateRequest request = new VisitRecordUpdateRequest(null, "updated", null, null);
        VisitRecordResponse result = new VisitRecordResponse(
            10L, 20L, 30L, "Alice", 2L, "Advisor Kim", 3L, "Main Store",
            LocalDateTime.of(2026, 8, 17, 14, 0), "purpose", "updated", null, null, null
        );
        when(service.updateVisitRecord(caUser, 10L, request)).thenReturn(result);

        var response = controller.updateVisitRecord(caUser, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(result);
    }

    @Test
    void deleteVisitRecordReturns200() {
        var response = controller.deleteVisitRecord(caUser, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
