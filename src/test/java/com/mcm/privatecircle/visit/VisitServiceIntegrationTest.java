package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.dto.VisitCreateRequest;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;
import com.mcm.privatecircle.visit.service.VisitService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VisitServiceIntegrationTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer customer;
    private Store caStore;
    private Store otherStore;
    private AuthenticatedUser caUser;

    @BeforeEach
    void setUp() {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount("visit-customer", passwordEncoder.encode("password123!"))
        );
        customer = customerRepository.save(new Customer(
            account,
            "C-VISIT",
            "방문 고객",
            "010-1111-2222",
            null,
            "GOLD",
            "qr-visit",
            "미니백",
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
        caStore = storeRepository.save(new Store("CA 매장", "서울"));
        otherStore = storeRepository.save(new Store("다른 매장", "부산"));
        caUser = AuthenticatedUser.ca(100L, 200L, caStore.getId());
    }

    @Test
    void 방문_생성은_Request가_아닌_Principal_매장을_사용한다() {
        LocalDateTime visitedAt = LocalDateTime.of(2026, 8, 17, 14, 0);

        var response = visitService.createVisit(
            caUser,
            new VisitCreateRequest(customer.getId(), visitedAt)
        );

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.storeId()).isEqualTo(caStore.getId());
        assertThat(response.visitedAt()).isEqualTo(visitedAt);
        assertThat(visitRepository.findById(response.visitId()))
            .get()
            .extracting(visit -> visit.getStore().getId())
            .isEqualTo(caStore.getId());
    }

    @Test
    void 같은_날_복수_방문을_허용한다() {
        visitService.createVisit(
            caUser,
            new VisitCreateRequest(customer.getId(), LocalDateTime.of(2026, 8, 17, 10, 0))
        );
        visitService.createVisit(
            caUser,
            new VisitCreateRequest(customer.getId(), LocalDateTime.of(2026, 8, 17, 18, 0))
        );

        assertThat(visitRepository.count()).isEqualTo(2);
    }

    @Test
    void 고객별_목록은_CA_매장만_최신순으로_페이지화한다() {
        Visit older = visitRepository.save(new Visit(
            customer,
            caStore,
            LocalDateTime.of(2026, 8, 10, 10, 0)
        ));
        Visit newer = visitRepository.save(new Visit(
            customer,
            caStore,
            LocalDateTime.of(2026, 8, 11, 10, 0)
        ));
        visitRepository.save(new Visit(
            customer,
            otherStore,
            LocalDateTime.of(2026, 8, 12, 10, 0)
        ));

        var response = visitService.getCustomerVisits(caUser, customer.getId(), 0, 20);

        assertThat(response.items())
            .extracting(item -> item.visitId())
            .containsExactly(newer.getId(), older.getId());
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void 타_매장_방문_상세는_존재를_노출하지_않는다() {
        Visit otherVisit = visitRepository.save(new Visit(
            customer,
            otherStore,
            LocalDateTime.of(2026, 8, 12, 10, 0)
        ));

        assertThatThrownBy(() -> visitService.getVisit(caUser, otherVisit.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void CUSTOMER는_CA_방문_API를_사용할_수_없다() {
        assertThatThrownBy(() -> visitService.createVisit(
            AuthenticatedUser.customer(1L, customer.getId()),
            new VisitCreateRequest(customer.getId(), LocalDateTime.of(2026, 8, 17, 10, 0))
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 페이지_범위가_잘못되면_INVALID_REQUEST다() {
        assertThatThrownBy(() -> visitService.getCustomerVisits(caUser, customer.getId(), -1, 20))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
