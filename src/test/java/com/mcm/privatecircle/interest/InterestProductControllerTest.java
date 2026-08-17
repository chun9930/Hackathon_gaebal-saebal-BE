package com.mcm.privatecircle.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.interest.controller.InterestProductController;
import com.mcm.privatecircle.interest.dto.CaInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.CustomerInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.InterestProductResponse;
import com.mcm.privatecircle.interest.service.InterestProductService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InterestProductControllerTest {

    private final InterestProductService service = mock(InterestProductService.class);
    private final InterestProductController controller = new InterestProductController(service);
    private final AuthenticatedUser customerUser = AuthenticatedUser.customer(1L, 2L);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 3L, 4L);

    @Test
    void CUSTOMER와_CA_생성은_201을_반환한다() {
        CustomerInterestCreateRequest customerRequest = new CustomerInterestCreateRequest(10L, "고객");
        CaInterestCreateRequest caRequest = new CaInterestCreateRequest(10L, 20L, "CA");
        InterestProductResponse result = mock(InterestProductResponse.class);
        when(service.createCustomerInterest(customerUser, customerRequest)).thenReturn(result);
        when(service.createCaInterest(caUser, 2L, caRequest)).thenReturn(result);

        assertThat(controller.createMyInterestProduct(customerUser, customerRequest).getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
        assertThat(controller.createCaInterestProduct(caUser, 2L, caRequest).getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 목록과_삭제는_200을_반환한다() {
        PageResponse<InterestProductResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, false);
        when(service.getMyInterestProducts(customerUser, 0, 20)).thenReturn(page);
        doNothing().when(service).deleteInterestProduct(customerUser, 30L);

        assertThat(controller.getMyInterestProducts(customerUser, 0, 20).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteInterestProduct(customerUser, 30L).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }
}
