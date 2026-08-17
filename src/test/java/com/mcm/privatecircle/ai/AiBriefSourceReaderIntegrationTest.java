package com.mcm.privatecircle.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.ai.service.AiBriefSourceReader;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.entity.InterestSourceType;
import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.purchase.entity.PurchaseHistory;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;
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
class AiBriefSourceReaderIntegrationTest {

    @Autowired
    private AiBriefSourceReader sourceReader;
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
    private ProductRepository productRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private VisitRecordRepository visitRecordRepository;
    @Autowired
    private CustomerInterestProductRepository interestRepository;
    @Autowired
    private PurchaseHistoryRepository purchaseRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer customer;
    private Customer otherCustomer;
    private Store store;
    private Store otherStore;
    private ClientAdvisor author;
    private ClientAdvisor otherStoreAuthor;
    private Product bag;
    private Product shoes;
    private Product watch;
    private AuthenticatedUser caUser;

    @BeforeEach
    void setUp() {
        customer = saveCustomer(
            "ai-customer",
            "C-AI-1",
            "010-5100-0001",
            "qr-ai-1",
            "VIP",
            "toned black"
        );
        otherCustomer = saveCustomer(
            "ai-other-customer",
            "C-AI-2",
            "010-5100-0002",
            "qr-ai-2",
            "GOLD",
            "bright color"
        );
        store = storeRepository.save(new Store("Gangnam", "Seoul"));
        otherStore = storeRepository.save(new Store("Busan", "Busan"));
        author = saveAdvisor("ai-author", store, "Author CA");
        otherStoreAuthor = saveAdvisor("ai-other-author", otherStore, "Other Store CA");
        bag = productRepository.save(new Product(
            "P-AI-1", "Himmel Bag", "Bag", null, new BigDecimal("1500000"), null, true
        ));
        shoes = productRepository.save(new Product(
            "P-AI-2", "Runner Shoes", "Shoes", null, new BigDecimal("900000"), null, true
        ));
        watch = productRepository.save(new Product(
            "P-AI-3", "Moon Watch", "Watch", null, new BigDecimal("2100000"), null, true
        ));
        caUser = AuthenticatedUser.ca(
            author.getEmployeeAccount().getId(),
            author.getId(),
            store.getId()
        );
    }

    @Test
    void readBuildsSourceFromPastCurrentStoreDataOnly() {
        Visit targetVisit = saveVisit(customer, store, LocalDateTime.of(2026, 8, 17, 12, 0));

        VisitRecord oldest = saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 10, 9, 0),
            "oldest visit",
            "too old to stay in top 5"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 12, 9, 0),
            "bag consult",
            "leather interest"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 13, 9, 0),
            "shoes consult",
            "comfort compare"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 14, 9, 0),
            "watch consult",
            "metal strap interest"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 15, 9, 0),
            "revisit",
            "color reconfirm"
        );
        VisitRecord newestSecond = saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 15, 9, 0),
            "latest consult",
            "final shortlist"
        );

        saveRecord(
            customer,
            otherStore,
            otherStoreAuthor,
            LocalDateTime.of(2026, 8, 16, 9, 0),
            "other store",
            "must be excluded"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 17, 12, 0),
            "same time",
            "must be excluded"
        );
        saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 18, 9, 0),
            "future visit",
            "must be excluded"
        );

        saveInterest(
            customer,
            bag,
            InterestSourceType.CUSTOMER,
            null,
            "customer common 1",
            LocalDateTime.of(2026, 8, 16, 8, 0)
        );
        saveInterest(
            customer,
            shoes,
            InterestSourceType.CUSTOMER,
            null,
            "customer common 2",
            LocalDateTime.of(2026, 8, 15, 8, 0)
        );
        saveInterest(
            customer,
            watch,
            InterestSourceType.CUSTOMER,
            null,
            "future customer interest",
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );
        saveInterest(
            customer,
            watch,
            InterestSourceType.CA,
            newestSecond,
            "same store ca interest",
            LocalDateTime.of(2026, 8, 16, 10, 0)
        );

        VisitRecord futureLinkedRecord = saveRecord(
            customer,
            store,
            author,
            LocalDateTime.of(2026, 8, 18, 11, 0),
            "future link",
            "future linked visit"
        );
        saveInterest(
            customer,
            bag,
            InterestSourceType.CA,
            futureLinkedRecord,
            "future linked ca interest",
            LocalDateTime.of(2026, 8, 16, 9, 30)
        );

        VisitRecord otherStoreLinkedRecord = saveRecord(
            customer,
            otherStore,
            otherStoreAuthor,
            LocalDateTime.of(2026, 8, 14, 11, 0),
            "other store link",
            "other store linked visit"
        );
        saveInterest(
            customer,
            shoes,
            InterestSourceType.CA,
            otherStoreLinkedRecord,
            "other store ca interest",
            LocalDateTime.of(2026, 8, 16, 9, 0)
        );

        saveInterest(
            otherCustomer,
            bag,
            InterestSourceType.CUSTOMER,
            null,
            "other customer",
            LocalDateTime.of(2026, 8, 16, 7, 0)
        );

        savePurchase(customer, bag, store, null, 1, LocalDateTime.of(2026, 8, 16, 14, 0));
        savePurchase(customer, shoes, store, targetVisit, 2, LocalDateTime.of(2026, 8, 15, 14, 0));
        savePurchase(customer, watch, otherStore, null, 1, LocalDateTime.of(2026, 8, 14, 14, 0));
        savePurchase(customer, watch, store, null, 1, LocalDateTime.of(2026, 8, 17, 12, 0));

        AiBriefSource source = sourceReader.read(caUser, customer.getId(), targetVisit.getId());

        assertThat(source.customer().membershipGrade()).isEqualTo("VIP");
        assertThat(source.customer().stylePreferences()).isEqualTo("toned black");
        assertThat(source.visitRecords())
            .extracting(AiBriefSource.VisitRecordSource::visitPurpose)
            .containsExactly("latest consult", "revisit", "watch consult", "shoes consult", "bag consult");
        assertThat(source.sourceVisitCount()).isEqualTo(5);
        assertThat(source.interestProducts())
            .extracting(AiBriefSource.InterestProductSource::memo)
            .containsExactly("same store ca interest", "customer common 1", "customer common 2");
        assertThat(source.purchases())
            .extracting(AiBriefSource.PurchaseSource::productName)
            .containsExactly("Himmel Bag", "Runner Shoes");
        assertThat(oldest.getVisitPurpose()).isNotIn(
            source.visitRecords().stream().map(AiBriefSource.VisitRecordSource::visitPurpose).toList()
        );
    }

    @Test
    void readThrowsVisitNotFoundWhenTargetVisitIsOutsideCurrentStore() {
        Visit otherStoreVisit = saveVisit(
            customer,
            otherStore,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );

        assertThatThrownBy(() -> sourceReader.read(caUser, customer.getId(), otherStoreVisit.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void readThrowsVisitCustomerMismatchWhenTargetVisitBelongsToAnotherCustomer() {
        Visit targetVisit = saveVisit(
            otherCustomer,
            store,
            LocalDateTime.of(2026, 8, 17, 12, 0)
        );

        assertThatThrownBy(() -> sourceReader.read(caUser, customer.getId(), targetVisit.getId()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_CUSTOMER_MISMATCH);
    }

    private Customer saveCustomer(
        String loginId,
        String customerNo,
        String phone,
        String qrToken,
        String membershipGrade,
        String stylePreferences
    ) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            "AI Customer",
            phone,
            null,
            membershipGrade,
            qrToken,
            stylePreferences,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
    }

    private ClientAdvisor saveAdvisor(String loginId, Store advisorStore, String name) {
        EmployeeAccount account = employeeAccountRepository.save(
            new EmployeeAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return clientAdvisorRepository.save(new ClientAdvisor(account, advisorStore, name));
    }

    private Visit saveVisit(Customer visitCustomer, Store visitStore, LocalDateTime visitedAt) {
        return visitRepository.save(new Visit(visitCustomer, visitStore, visitedAt));
    }

    private VisitRecord saveRecord(
        Customer recordCustomer,
        Store recordStore,
        ClientAdvisor ca,
        LocalDateTime visitedAt,
        String visitPurpose,
        String content
    ) {
        Visit visit = saveVisit(recordCustomer, recordStore, visitedAt);
        return visitRecordRepository.save(new VisitRecord(
            visit,
            recordCustomer,
            ca,
            visitPurpose,
            content,
            "style note",
            "caution note"
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
            interestCustomer,
            interestProduct,
            sourceType,
            visitRecord,
            memo,
            savedAt
        ));
    }

    private PurchaseHistory savePurchase(
        Customer purchaseCustomer,
        Product purchaseProduct,
        Store purchaseStore,
        Visit visit,
        int quantity,
        LocalDateTime purchasedAt
    ) {
        return purchaseRepository.save(new PurchaseHistory(
            purchaseCustomer,
            purchaseProduct,
            purchaseStore,
            visit,
            quantity,
            purchasedAt
        ));
    }
}
