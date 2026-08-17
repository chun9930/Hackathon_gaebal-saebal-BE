package com.mcm.privatecircle.stamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.stamp.dto.VisitStampCreateRequest;
import com.mcm.privatecircle.stamp.entity.VisitStamp;
import com.mcm.privatecircle.stamp.repository.VisitStampRepository;
import com.mcm.privatecircle.stamp.service.VisitStampService;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VisitStampServiceIntegrationTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-08-17T04:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    @Autowired
    private VisitStampRepository stampRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CustomerAccountRepository customerAccountRepository;
    @Autowired
    private EmployeeAccountRepository employeeAccountRepository;
    @Autowired
    private ClientAdvisorRepository clientAdvisorRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private VisitStampService service;
    private Customer customer;
    private Customer otherCustomer;
    private Store store;
    private Store otherStore;
    private ClientAdvisor ca;
    private ClientAdvisor otherStoreCa;
    private AuthenticatedUser caUser;
    private AuthenticatedUser customerUser;

    @BeforeEach
    void setUp() {
        service = new VisitStampService(
            stampRepository,
            visitRepository,
            customerRepository,
            clientAdvisorRepository,
            FIXED_CLOCK
        );
        customer = saveCustomer("stamp-customer", "C-S-1", "010-5100-0001", "qr-s-1");
        otherCustomer = saveCustomer("stamp-other", "C-S-2", "010-5100-0002", "qr-s-2");
        store = storeRepository.save(new Store("서울점", "서울"));
        otherStore = storeRepository.save(new Store("부산점", "부산"));
        ca = saveAdvisor("stamp-ca", store, "서울 CA");
        otherStoreCa = saveAdvisor("stamp-other-ca", otherStore, "부산 CA");
        caUser = AuthenticatedUser.ca(ca.getEmployeeAccount().getId(), ca.getId(), store.getId());
        customerUser = AuthenticatedUser.customer(
            customer.getCustomerAccount().getId(), customer.getId()
        );
    }

    @Test
    void 스탬프는_Visit_고객_Principal_CA_공통_Clock으로_발급하고_방문별_중복을_막는다() {
        Visit visit = saveVisit(customer, store, LocalDateTime.of(2026, 8, 17, 10, 0));
        VisitStampCreateRequest request = new VisitStampCreateRequest("VISIT");

        var response = service.issueStamp(caUser, visit.getId(), request);

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.issuedByCaId()).isEqualTo(ca.getId());
        assertThat(response.issuedAt()).isEqualTo(LocalDateTime.of(2026, 8, 17, 13, 0));
        assertThatThrownBy(() -> service.issueStamp(caUser, visit.getId(), request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STAMP_ALREADY_ISSUED);
    }

    @Test
    void 타_매장_Visit에는_스탬프를_발급할_수_없다() {
        Visit visit = saveVisit(customer, otherStore, LocalDateTime.of(2026, 8, 17, 10, 0));

        assertThatThrownBy(() -> service.issueStamp(
            caUser, visit.getId(), new VisitStampCreateRequest("VISIT")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void stampType은_필수이고_최대_30자다() {
        Visit visit = saveVisit(customer, store, LocalDateTime.of(2026, 8, 17, 10, 0));

        assertThatThrownBy(() -> service.issueStamp(
            caUser, visit.getId(), new VisitStampCreateRequest(" ")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> service.issueStamp(
            caUser, visit.getId(), new VisitStampCreateRequest("A".repeat(31))
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void CA_조회는_자기_매장_스탬프만_시간과_ID_내림차순으로_페이지화한다() {
        VisitStamp older = saveStamp(customer, store, ca, LocalDateTime.of(2026, 8, 15, 10, 0));
        VisitStamp newerFirst = saveStamp(customer, store, ca, LocalDateTime.of(2026, 8, 16, 10, 0));
        VisitStamp newerSecond = saveStamp(customer, store, ca, LocalDateTime.of(2026, 8, 16, 10, 0));
        saveStamp(customer, otherStore, otherStoreCa, LocalDateTime.of(2026, 8, 17, 10, 0));

        var page = service.getCustomerStamps(caUser, customer.getId(), 0, 20);

        assertThat(page.items()).extracting("stampId")
            .containsExactly(newerSecond.getId(), newerFirst.getId(), older.getId());
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void CUSTOMER_조회는_본인의_전체_매장_스탬프만_반환한다() {
        saveStamp(customer, store, ca, LocalDateTime.of(2026, 8, 15, 10, 0));
        saveStamp(customer, otherStore, otherStoreCa, LocalDateTime.of(2026, 8, 16, 10, 0));
        saveStamp(otherCustomer, store, ca, LocalDateTime.of(2026, 8, 17, 10, 0));

        var page = service.getMyStamps(customerUser, 0, 20);

        assertThat(page.items()).hasSize(2);
        assertThat(page.items()).extracting("customerId").containsOnly(customer.getId());
    }

    @Test
    void 스탬프_목록_페이지_범위를_검증한다() {
        assertThatThrownBy(() -> service.getMyStamps(customerUser, 0, 101))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private VisitStamp saveStamp(
        Customer stampCustomer,
        Store stampStore,
        ClientAdvisor issuedBy,
        LocalDateTime issuedAt
    ) {
        Visit visit = saveVisit(stampCustomer, stampStore, issuedAt.minusHours(1));
        return stampRepository.saveAndFlush(new VisitStamp(
            visit, stampCustomer, issuedBy, "VISIT", issuedAt
        ));
    }

    private Visit saveVisit(Customer visitCustomer, Store visitStore, LocalDateTime visitedAt) {
        return visitRepository.save(new Visit(visitCustomer, visitStore, visitedAt));
    }

    private ClientAdvisor saveAdvisor(String loginId, Store advisorStore, String name) {
        EmployeeAccount account = employeeAccountRepository.save(
            new EmployeeAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return clientAdvisorRepository.save(new ClientAdvisor(account, advisorStore, name));
    }

    private Customer saveCustomer(String loginId, String customerNo, String phone, String qrToken) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            "스탬프 고객",
            phone,
            null,
            "GOLD",
            qrToken,
            null,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
    }
}
