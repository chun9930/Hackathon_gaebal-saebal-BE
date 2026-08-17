package com.mcm.privatecircle.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.customer.service.CustomerService;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.stamp.entity.VisitStamp;
import com.mcm.privatecircle.stamp.repository.VisitStampRepository;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CustomerActivitySummaryIntegrationTest {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private CustomerAccountRepository customerAccountRepository;
    @Autowired
    private EmployeeAccountRepository employeeAccountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ClientAdvisorRepository clientAdvisorRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private VisitStampRepository stampRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 방문수_스탬프수_최근방문일을_B_Repository에서_집계한다() {
        CustomerAccount customerAccount = customerAccountRepository.save(
            new CustomerAccount("summary-customer", passwordEncoder.encode("password123!"))
        );
        Customer customer = customerRepository.save(new Customer(
            customerAccount,
            "C-SUMMARY",
            "집계 고객",
            "010-6100-0001",
            null,
            "GOLD",
            "qr-summary",
            null,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
        Store store = storeRepository.save(new Store("집계 매장", "서울"));
        EmployeeAccount employeeAccount = employeeAccountRepository.save(
            new EmployeeAccount("summary-ca", passwordEncoder.encode("password123!"))
        );
        ClientAdvisor ca = clientAdvisorRepository.save(
            new ClientAdvisor(employeeAccount, store, "집계 CA")
        );
        Visit older = visitRepository.save(new Visit(
            customer, store, LocalDateTime.of(2026, 8, 15, 10, 0)
        ));
        visitRepository.save(new Visit(
            customer, store, LocalDateTime.of(2026, 8, 16, 11, 0)
        ));
        stampRepository.save(new VisitStamp(
            older, customer, ca, "VISIT", LocalDateTime.of(2026, 8, 15, 10, 5)
        ));

        var profile = customerService.getCustomerDetail(customer.getId());

        assertThat(profile.visitCount()).isEqualTo(2);
        assertThat(profile.stampCount()).isEqualTo(1);
        assertThat(profile.lastVisitedAt()).isEqualTo(LocalDateTime.of(2026, 8, 16, 11, 0));
    }
}
