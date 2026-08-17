package com.mcm.privatecircle.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.purchase.controller.PurchaseController;
import com.mcm.privatecircle.purchase.dto.PurchaseCreateRequest;
import com.mcm.privatecircle.purchase.dto.PurchaseResponse;
import com.mcm.privatecircle.purchase.service.PurchaseService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PurchaseControllerTest {

    private final PurchaseService service = mock(PurchaseService.class);
    private final PurchaseController controller = new PurchaseController(service);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void 구매_생성은_201을_반환한다() {
        PurchaseCreateRequest request = new PurchaseCreateRequest(
            10L, 20L, null, 1, LocalDateTime.of(2026, 8, 17, 10, 0)
        );
        PurchaseResponse result = mock(PurchaseResponse.class);
        when(service.createPurchase(caUser, request)).thenReturn(result);

        assertThat(controller.createPurchase(caUser, request).getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 구매_목록은_200과_PageResponse를_반환한다() {
        PageResponse<PurchaseResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, false);
        when(service.getCustomerPurchases(caUser, 10L, 0, 20)).thenReturn(page);

        var response = controller.getCustomerPurchases(caUser, 10L, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(page);
    }
}
