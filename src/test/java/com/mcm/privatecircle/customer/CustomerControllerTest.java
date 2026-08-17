package com.mcm.privatecircle.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.customer.controller.CustomerController;
import com.mcm.privatecircle.customer.dto.CustomerSearchResponse;
import com.mcm.privatecircle.customer.service.CustomerService;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CustomerControllerTest {

    private final CustomerService customerService = org.mockito.Mockito.mock(CustomerService.class);
    private final CustomerController controller = new CustomerController(customerService);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void searchCustomersReturnsPageResponseWith200() {
        PageResponse<CustomerSearchResponse> page = new PageResponse<>(
            List.of(new CustomerSearchResponse(
                10L,
                "C00000010",
                "Hong Gil Dong",
                "01012345678",
                "https://img.example.com/customer.png",
                "GOLD",
                LocalDateTime.of(2026, 8, 1, 10, 0)
            )),
            0,
            20,
            1,
            1,
            false
        );
        when(customerService.searchCustomers(caUser, "Hong", 0, 20)).thenReturn(page);

        var response = controller.searchCustomers(caUser, "Hong", 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo(page);
        verify(customerService).searchCustomers(caUser, "Hong", 0, 20);
    }
}