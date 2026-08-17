package com.mcm.privatecircle.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.customer.service.CustomerService;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerSearchIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AuthenticatedUser caUser = AuthenticatedUser.ca(100L, 200L, 300L);

    @Test
    void searchCustomersMatchesNamePhoneAndCustomerNo() {
        Customer alpha = saveCustomer("search-customer-1", "C00001001", "Alice Kim", "01011112222");
        Customer beta = saveCustomer("search-customer-2", "C00001002", "Brian Lee", "01033334444");
        saveCustomer("search-customer-3", "C00001003", "Charlie Park", "01055556666");

        var byName = customerService.searchCustomers(caUser, "Alice", 0, 20);
        var byPhone = customerService.searchCustomers(caUser, "3333", 0, 20);
        var byCustomerNo = customerService.searchCustomers(caUser, "1003", 0, 20);

        assertThat(byName.items()).hasSize(1);
        assertThat(byName.items().get(0).customerId()).isEqualTo(alpha.getId());
        assertThat(byName.items().get(0).customerNo()).isEqualTo("C00001001");

        assertThat(byPhone.items()).hasSize(1);
        assertThat(byPhone.items().get(0).customerId()).isEqualTo(beta.getId());

        assertThat(byCustomerNo.items()).hasSize(1);
        assertThat(byCustomerNo.items().get(0).customerNo()).isEqualTo("C00001003");
    }

    @Test
    void searchCustomersAppliesPagingAndNewestFirstOrdering() {
        saveCustomer("search-page-1", "C00002001", "Paged One", "01070000001");
        saveCustomer("search-page-2", "C00002002", "Paged Two", "01070000002");
        saveCustomer("search-page-3", "C00002003", "Paged Three", "01070000003");

        var page = customerService.searchCustomers(caUser, "Paged", 0, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.page()).isEqualTo(0);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.items().get(0).customerNo()).isEqualTo("C00002003");
        assertThat(page.items().get(1).customerNo()).isEqualTo("C00002002");
    }

    @Test
    void searchCustomersRejectsBlankKeyword() {
        assertThatThrownBy(() -> customerService.searchCustomers(caUser, "   ", 0, 20))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private Customer saveCustomer(String loginId, String customerNo, String name, String phoneNumber) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            name,
            phoneNumber,
            null,
            "GOLD",
            "qr-" + loginId,
            "classic",
            LocalDateTime.of(2026, 8, 1, 10, 0).plusMinutes(customerRepository.count())
        ));
    }
}