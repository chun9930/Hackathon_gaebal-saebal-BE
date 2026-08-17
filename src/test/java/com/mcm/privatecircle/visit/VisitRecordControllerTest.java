package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    void 방문_기록_생성은_201을_반환한다() {
        VisitRecordCreateRequest request =
            new VisitRecordCreateRequest("목적", "내용", null, null);
        VisitRecordResponse result =
            new VisitRecordResponse(10L, 20L, 30L, 2L, "목적", "내용", null, null, null);
        when(service.createVisitRecord(caUser, 20L, request)).thenReturn(result);

        var response = controller.createVisitRecord(caUser, 20L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData()).isEqualTo(result);
    }

    @Test
    void 방문_기록_수정은_200을_반환한다() {
        VisitRecordUpdateRequest request =
            new VisitRecordUpdateRequest(null, "수정", null, null);
        VisitRecordResponse result =
            new VisitRecordResponse(10L, 20L, 30L, 2L, "목적", "수정", null, null, null);
        when(service.updateVisitRecord(caUser, 10L, request)).thenReturn(result);

        var response = controller.updateVisitRecord(caUser, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(result);
    }
}
