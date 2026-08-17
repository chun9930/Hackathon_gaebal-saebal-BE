package com.mcm.privatecircle.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
import com.mcm.privatecircle.interest.dto.CaInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.CustomerInterestCreateRequest;
import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.entity.InterestSourceType;
import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.interest.service.InterestProductService;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InterestProductServiceIntegrationTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-08-17T03:00:00Z"),
        SEOUL
    );

    @Autowired
    private CustomerInterestProductRepository interestRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private VisitRecordRepository visitRecordRepository;
    @Autowired
    private VisitRepository visitRepository;
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

    private InterestProductService service;
    private Customer customer;
    private Customer otherCustomer;
    private Product product;
    private Store store;
    private Store otherStore;
    private ClientAdvisor author;
    private ClientAdvisor colleague;
    private ClientAdvisor otherStoreAuthor;
    private AuthenticatedUser customerUser;
    private AuthenticatedUser authorUser;

    @BeforeEach
    void setUp() {
        service = new InterestProductService(
            interestRepository,
            customerRepository,
            productRepository,
            visitRecordRepository,
            FIXED_CLOCK
        );
        customer = saveCustomer("interest-customer", "C-I-1", "010-3100-0001", "qr-i-1");
        otherCustomer = saveCustomer("interest-other", "C-I-2", "010-3100-0002", "qr-i-2");
        product = productRepository.save(new Product(
            "P-I-1", "Himmel Bag", "Bag", null, new BigDecimal("1500000"), null, true
        ));
        store = storeRepository.save(new Store("강남점", "서울"));
        otherStore = storeRepository.save(new Store("부산점", "부산"));
        author = saveAdvisor("interest-author", store, "작성 CA");
        colleague = saveAdvisor("interest-colleague", store, "동료 CA");
        otherStoreAuthor = saveAdvisor("interest-other-ca", otherStore, "타 매장 CA");
        customerUser = AuthenticatedUser.customer(
            customer.getCustomerAccount().getId(), customer.getId()
        );
        authorUser = AuthenticatedUser.ca(
            author.getEmployeeAccount().getId(), author.getId(), store.getId()
        );
    }

    @Test
    void CUSTOMER_저장은_Principal_고객과_공통_Clock을_사용하고_고객_상품_중복을_막는다() {
        CustomerInterestCreateRequest request = new CustomerInterestCreateRequest(product.getId(), "직접 저장");

        var response = service.createCustomerInterest(customerUser, request);

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.sourceType()).isEqualTo(InterestSourceType.CUSTOMER);
        assertThat(response.visitRecordId()).isNull();
        assertThat(response.savedAt()).isEqualTo(LocalDateTime.of(2026, 8, 17, 12, 0));
        assertThatThrownBy(() -> service.createCustomerInterest(customerUser, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
    }

    @Test
    void CA_저장은_같은_VisitRecord와_상품만_중복이고_다른_VisitRecord의_동일_상품은_허용한다() {
        VisitRecord first = saveRecord(customer, store, author, LocalDateTime.of(2026, 8, 15, 10, 0));
        VisitRecord second = saveRecord(customer, store, author, LocalDateTime.of(2026, 8, 16, 10, 0));
        CaInterestCreateRequest firstRequest = new CaInterestCreateRequest(product.getId(), first.getId(), "첫 방문");

        service.createCaInterest(authorUser, customer.getId(), firstRequest);

        assertThatThrownBy(() -> service.createCaInterest(authorUser, customer.getId(), firstRequest))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
        assertThat(service.createCaInterest(
            authorUser,
            customer.getId(),
            new CaInterestCreateRequest(product.getId(), second.getId(), "다음 방문")
        ).visitRecordId()).isEqualTo(second.getId());
    }

    @Test
    void CA_저장은_방문기록_고객과_Path_고객이_다르면_거절한다() {
        VisitRecord record = saveRecord(
            customer, store, author, LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        assertThatThrownBy(() -> service.createCaInterest(
            authorUser,
            otherCustomer.getId(),
            new CaInterestCreateRequest(product.getId(), record.getId(), "불일치")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INTEREST_SOURCE);
    }

    @Test
    void CA_저장은_타_매장_방문기록을_존재하지_않는_것처럼_차단한다() {
        VisitRecord otherStoreRecord = saveRecord(
            customer, otherStore, otherStoreAuthor, LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        assertThatThrownBy(() -> service.createCaInterest(
            authorUser,
            customer.getId(),
            new CaInterestCreateRequest(product.getId(), otherStoreRecord.getId(), "타 매장")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_RECORD_NOT_FOUND);
    }
    @Test
    void CA_조회는_CUSTOMER와_자기_매장_CA_항목만_하나의_정렬_페이지로_반환한다() {
        VisitRecord sameStoreRecord = saveRecord(
            customer, store, author, LocalDateTime.of(2026, 8, 14, 10, 0)
        );
        VisitRecord otherStoreRecord = saveRecord(
            customer, otherStore, otherStoreAuthor, LocalDateTime.of(2026, 8, 14, 11, 0)
        );
        saveInterest(customer, product, InterestSourceType.CUSTOMER, null, "고객", LocalDateTime.of(2026, 8, 10, 10, 0));
        saveInterest(customer, product, InterestSourceType.CA, sameStoreRecord, "같은 매장", LocalDateTime.of(2026, 8, 12, 10, 0));
        saveInterest(customer, product, InterestSourceType.CA, otherStoreRecord, "타 매장", LocalDateTime.of(2026, 8, 13, 10, 0));
        saveInterest(otherCustomer, product, InterestSourceType.CUSTOMER, null, "다른 고객", LocalDateTime.of(2026, 8, 14, 10, 0));

        var page = service.getCustomerInterestProducts(authorUser, customer.getId(), 0, 20);

        assertThat(page.items()).extracting("memo").containsExactly("같은 매장", "고객");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void CUSTOMER_조회는_본인의_CUSTOMER_출처만_반환한다() {
        VisitRecord record = saveRecord(customer, store, author, LocalDateTime.of(2026, 8, 14, 10, 0));
        saveInterest(customer, product, InterestSourceType.CUSTOMER, null, "내 항목", LocalDateTime.of(2026, 8, 10, 10, 0));
        saveInterest(customer, product, InterestSourceType.CA, record, "CA 항목", LocalDateTime.of(2026, 8, 11, 10, 0));

        var page = service.getMyInterestProducts(customerUser, 0, 20);

        assertThat(page.items()).extracting("memo").containsExactly("내 항목");
    }

    @Test
    void CA_출처_삭제는_연결_VisitRecord의_최초_작성_CA만_허용한다() {
        VisitRecord record = saveRecord(customer, store, author, LocalDateTime.of(2026, 8, 14, 10, 0));
        CustomerInterestProduct interest = saveInterest(
            customer, product, InterestSourceType.CA, record, "삭제 대상", LocalDateTime.of(2026, 8, 15, 10, 0)
        );
        AuthenticatedUser colleagueUser = AuthenticatedUser.ca(
            colleague.getEmployeeAccount().getId(), colleague.getId(), store.getId()
        );

        assertThatThrownBy(() -> service.deleteInterestProduct(colleagueUser, interest.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN_CA);

        service.deleteInterestProduct(authorUser, interest.getId());
        assertThat(interestRepository.findById(interest.getId())).isEmpty();
    }

    @Test
    void 행_부재는_404_행은_있지만_삭제_권한이_없으면_403으로_구분한다() {
        CustomerInterestProduct otherInterest = saveInterest(
            otherCustomer,
            product,
            InterestSourceType.CUSTOMER,
            null,
            "다른 고객",
            LocalDateTime.of(2026, 8, 15, 10, 0)
        );

        assertThatThrownBy(() -> service.deleteInterestProduct(customerUser, 999999L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INTEREST_PRODUCT_NOT_FOUND);
        assertThatThrownBy(() -> service.deleteInterestProduct(customerUser, otherInterest.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 페이지_범위를_벗어나면_INVALID_REQUEST를_반환한다() {
        assertThatThrownBy(() -> service.getMyInterestProducts(customerUser, -1, 20))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> service.getCustomerInterestProducts(authorUser, customer.getId(), 0, 101))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private Customer saveCustomer(String loginId, String customerNo, String phone, String qrToken) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            "관심 고객",
            phone,
            null,
            "GOLD",
            qrToken,
            null,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
    }

    private ClientAdvisor saveAdvisor(String loginId, Store advisorStore, String name) {
        EmployeeAccount account = employeeAccountRepository.save(
            new EmployeeAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return clientAdvisorRepository.save(new ClientAdvisor(account, advisorStore, name));
    }

    private VisitRecord saveRecord(
        Customer recordCustomer,
        Store recordStore,
        ClientAdvisor ca,
        LocalDateTime visitedAt
    ) {
        Visit visit = visitRepository.save(new Visit(recordCustomer, recordStore, visitedAt));
        return visitRecordRepository.save(new VisitRecord(
            visit, recordCustomer, ca, "목적", "내용", null, null
        ));
    }

    private CustomerInterestProduct saveInterest(
        Customer interestCustomer,
        Product interestProduct,
        InterestSourceType sourceType,
        VisitRecord visitRecord,
        String memo,
        LocalDateTime savedAt
    ) {
        return interestRepository.save(new CustomerInterestProduct(
            interestCustomer, interestProduct, sourceType, visitRecord, memo, savedAt
        ));
    }
}
