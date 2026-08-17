package com.mcm.privatecircle.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.mcm.privatecircle.customer.dto.CustomerActivitySummary;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.customer.service.CustomerActivitySummaryReader;
import com.mcm.privatecircle.customer.service.CustomerService;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class CustomerServiceActivityFailureTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerActivitySummaryReader summaryReader = mock(CustomerActivitySummaryReader.class);
    private final CustomerService service = new CustomerService(customerRepository, summaryReader);

    @Test
    void 실제_0건은_0과_null_계산값으로_반환한다() {
        Customer customer = customer();
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(summaryReader.read(10L)).thenReturn(CustomerActivitySummary.empty());

        var response = service.getCustomerDetail(10L);

        assertThat(response.visitCount()).isZero();
        assertThat(response.stampCount()).isZero();
        assertThat(response.lastVisitedAt()).isNull();
    }

    @Test
    void DB_장애를_0과_null로_숨기지_않는다() {
        Customer customer = customer();
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("DB down");
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(summaryReader.read(10L)).thenThrow(failure);

        assertThatThrownBy(() -> service.getCustomerDetail(10L)).isSameAs(failure);
    }

    private Customer customer() {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(10L);
        when(customer.getJoinedAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 10, 0));
        return customer;
    }
}
