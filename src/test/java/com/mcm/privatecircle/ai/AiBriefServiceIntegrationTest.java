package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
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
class AiBriefServiceIntegrationTest {

    @Autowired
    private AiBriefService service;
    @Autowired
    private AiJourneyBriefRepository repository;
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
    private VisitRepository visitRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer customer;
    private Customer otherCustomer;
    private Store store;
    private Store otherStore;
    private ClientAdvisor author;
    private ClientAdvisor otherStoreAuthor;
    private AuthenticatedUser caUser;

    @BeforeEach
    void setUp() {
        customer = saveCustomer("ai-brief-customer", "C-AIB-1", "010-6100-0001", "qr-aib-1");
        otherCustomer = saveCustomer("ai-brief-customer-2", "C-AIB-2", "010-6100-0002", "qr-aib-2");
        store = storeRepository.save(new Store("Gangnam", "Seoul"));
        otherStore = storeRepository.save(new Store("Busan", "Busan"));
        author = saveAdvisor("ai-brief-author", store, "Author CA");
        otherStoreAuthor = saveAdvisor("ai-brief-other-author", otherStore, "Other Store CA");
        caUser = AuthenticatedUser.ca(
            author.getEmployeeAccount().getId(),
            author.getId(),
            store.getId()
        );
    }

    @Test
    void getLatestReturnsMostRecentAttemptIncludingFailed() {
        Visit targetVisit = visitRepository.save(new Visit(
            customer,
            store,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        ));
        repository.save(new AiJourneyBrief(
            null,
            customer,
            targetVisit,
            author,
            "generated old",
            "purpose",
            "interest",
            "caution",
            "direction",
            2,
            BriefStatus.GENERATED,
            LocalDateTime.of(2026, 8, 17, 11, 0)
        ));
        AiJourneyBrief latest = repository.save(new AiJourneyBrief(
            null,
            customer,
            targetVisit,
            author,
            null,
            null,
            null,
            null,
            null,
            2,
            BriefStatus.FAILED,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        ));

        var response = service.getLatest(caUser, customer.getId(), targetVisit.getId());

        assertThat(response.briefId()).isEqualTo(latest.getId());
        assertThat(response.status()).isEqualTo(BriefStatus.FAILED);
    }

    @Test
    void getLatestThrowsNotFoundWhenNoAttemptExists() {
        Visit targetVisit = visitRepository.save(new Visit(
            customer,
            store,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        ));

        assertThatThrownBy(() -> service.getLatest(caUser, customer.getId(), targetVisit.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.AI_BRIEF_NOT_FOUND);
    }

    @Test
    void getHistoryFiltersByCurrentStoreAndSortsByGeneratedAtDescIdDesc() {
        Visit firstVisit = visitRepository.save(new Visit(
            customer,
            store,
            LocalDateTime.of(2026, 8, 16, 12, 0)
        ));
        Visit secondVisit = visitRepository.save(new Visit(
            customer,
            store,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        ));
        Visit otherStoreVisit = visitRepository.save(new Visit(
            customer,
            otherStore,
            LocalDateTime.of(2026, 8, 17, 13, 0)
        ));

        AiJourneyBrief older = repository.save(new AiJourneyBrief(
            null, customer, firstVisit, author, "older", "p", "i", "c", "d", 1,
            BriefStatus.GENERATED, LocalDateTime.of(2026, 8, 16, 12, 0)
        ));
        AiJourneyBrief newerFirst = repository.save(new AiJourneyBrief(
            null, customer, secondVisit, author, "newer-1", "p", "i", "c", "d", 2,
            BriefStatus.GENERATED, LocalDateTime.of(2026, 8, 17, 12, 0)
        ));
        AiJourneyBrief newerSecond = repository.save(new AiJourneyBrief(
            null, customer, secondVisit, author, "newer-2", "p", "i", "c", "d", 3,
            BriefStatus.FAILED, LocalDateTime.of(2026, 8, 17, 12, 0)
        ));
        repository.save(new AiJourneyBrief(
            null, customer, otherStoreVisit, otherStoreAuthor, "other-store", "p", "i", "c", "d", 4,
            BriefStatus.GENERATED, LocalDateTime.of(2026, 8, 17, 14, 0)
        ));

        var page = service.getHistory(caUser, customer.getId(), 0, 20);

        assertThat(page.items()).extracting("briefId")
            .containsExactly(newerSecond.getId(), newerFirst.getId(), older.getId());
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void getLatestRejectsVisitOfAnotherCustomer() {
        Visit targetVisit = visitRepository.save(new Visit(
            otherCustomer,
            store,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        ));

        assertThatThrownBy(() -> service.getLatest(caUser, customer.getId(), targetVisit.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_CUSTOMER_MISMATCH);
    }

    private Customer saveCustomer(String loginId, String customerNo, String phone, String qrToken) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            "AI Brief Customer",
            phone,
            null,
            "VIP",
            qrToken,
            "black",
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
    }

    private ClientAdvisor saveAdvisor(String loginId, Store advisorStore, String name) {
        EmployeeAccount account = employeeAccountRepository.save(
            new EmployeeAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return clientAdvisorRepository.save(new ClientAdvisor(account, advisorStore, name));
    }
}
