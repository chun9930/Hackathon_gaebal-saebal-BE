package com.mcm.privatecircle.visit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

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
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.dto.VisitRecordCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitRecordUpdateRequest;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;
import com.mcm.privatecircle.visit.service.VisitRecordService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VisitRecordServiceIntegrationTest {

    @Autowired
    private VisitRecordService visitRecordService;

    @Autowired
    private VisitRecordRepository visitRecordRepository;

    @Autowired
    private VisitRepository visitRepository;

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
    private PasswordEncoder passwordEncoder;

    private Customer customer;
    private Store store;
    private Store otherStore;
    private ClientAdvisor author;
    private ClientAdvisor colleague;
    private AuthenticatedUser authorUser;

    @BeforeEach
    void setUp() {
        CustomerAccount customerAccount = customerAccountRepository.save(
            new CustomerAccount("record-customer", passwordEncoder.encode("password123!"))
        );
        customer = customerRepository.save(new Customer(
            customerAccount,
            "C-RECORD",
            "기록 고객",
            "010-2222-3333",
            null,
            "GOLD",
            "qr-record",
            null,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
        store = storeRepository.save(new Store("기록 매장", "서울"));
        otherStore = storeRepository.save(new Store("타 매장", "부산"));
        author = saveAdvisor("record-author", store, "작성 CA");
        colleague = saveAdvisor("record-colleague", store, "동료 CA");
        authorUser = AuthenticatedUser.ca(
            author.getEmployeeAccount().getId(),
            author.getId(),
            store.getId()
        );
    }

    @Test
    void 방문에서_고객을_Principal에서_CA를_파생해_기록을_생성한다() {
        Visit visit = saveVisit(store);

        var response = visitRecordService.createVisitRecord(
            authorUser,
            visit.getId(),
            new VisitRecordCreateRequest("신상품 확인", "고객 반응", "밝은 컬러", "알레르기")
        );

        assertThat(response.visitId()).isEqualTo(visit.getId());
        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.caId()).isEqualTo(author.getId());
        assertThat(visitRecordRepository.findById(response.visitRecordId())).isPresent();
    }

    @Test
    void 한_방문에_기록은_하나만_허용한다() {
        Visit visit = saveVisit(store);
        VisitRecordCreateRequest request =
            new VisitRecordCreateRequest("목적", "내용", null, null);
        visitRecordService.createVisitRecord(authorUser, visit.getId(), request);

        assertThatThrownBy(() -> visitRecordService.createVisitRecord(authorUser, visit.getId(), request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_RECORD_ALREADY_EXISTS);
    }

    @Test
    void 최초_작성_CA만_기록을_수정할_수_있다() {
        Visit visit = saveVisit(store);
        var created = visitRecordService.createVisitRecord(
            authorUser,
            visit.getId(),
            new VisitRecordCreateRequest("목적", "원본", "기존", null)
        );
        AuthenticatedUser colleagueUser = AuthenticatedUser.ca(
            colleague.getEmployeeAccount().getId(),
            colleague.getId(),
            store.getId()
        );

        assertThatThrownBy(() -> visitRecordService.updateVisitRecord(
            colleagueUser,
            created.visitRecordId(),
            new VisitRecordUpdateRequest(null, "변경 시도", null, null)
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN_CA);

        var updated = visitRecordService.updateVisitRecord(
            authorUser,
            created.visitRecordId(),
            new VisitRecordUpdateRequest(null, "수정 내용", null, null)
        );
        assertThat(updated.content()).isEqualTo("수정 내용");
        assertThat(updated.visitPurpose()).isEqualTo("목적");
    }

    @Test
    void PATCH에_변경_필드가_없으면_INVALID_REQUEST다() {
        Visit visit = saveVisit(store);
        var created = visitRecordService.createVisitRecord(
            authorUser,
            visit.getId(),
            new VisitRecordCreateRequest("목적", null, null, null)
        );

        assertThatThrownBy(() -> visitRecordService.updateVisitRecord(
            authorUser,
            created.visitRecordId(),
            new VisitRecordUpdateRequest(null, null, null, null)
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 타_매장_방문에는_기록을_생성할_수_없다() {
        Visit otherVisit = saveVisit(otherStore);

        assertThatThrownBy(() -> visitRecordService.createVisitRecord(
            authorUser,
            otherVisit.getId(),
            new VisitRecordCreateRequest("목적", null, null, null)
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
    }

    private Visit saveVisit(Store visitStore) {
        return visitRepository.save(new Visit(
            customer,
            visitStore,
            LocalDateTime.of(2026, 8, 17, 14, 0)
        ));
    }

    private ClientAdvisor saveAdvisor(String loginId, Store advisorStore, String name) {
        EmployeeAccount account = employeeAccountRepository.save(
            new EmployeeAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return clientAdvisorRepository.save(new ClientAdvisor(account, advisorStore, name));
    }
}
