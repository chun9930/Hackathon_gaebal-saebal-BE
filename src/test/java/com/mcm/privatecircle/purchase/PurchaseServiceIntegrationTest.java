package com.mcm.privatecircle.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.account.repository.CustomerAccountRepository;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.purchase.dto.PurchaseCreateRequest;
import com.mcm.privatecircle.purchase.entity.PurchaseHistory;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;
import com.mcm.privatecircle.purchase.service.PurchaseService;
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
class PurchaseServiceIntegrationTest {

    @Autowired
    private PurchaseHistoryRepository purchaseRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private CustomerAccountRepository customerAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private PurchaseService service;
    private Customer customer;
    private Customer otherCustomer;
    private Product product;
    private Store store;
    private Store otherStore;
    private AuthenticatedUser caUser;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(
            purchaseRepository,
            customerRepository,
            productRepository,
            storeRepository,
            visitRepository
        );
        customer = saveCustomer("purchase-customer", "C-P-1", "010-4100-0001", "qr-p-1");
        otherCustomer = saveCustomer("purchase-other", "C-P-2", "010-4100-0002", "qr-p-2");
        product = productRepository.save(new Product(
            "P-P-1", "Boston Bag", "Bag", null, new BigDecimal("2000000"), null, true
        ));
        store = storeRepository.save(new Store("서울점", "서울"));
        otherStore = storeRepository.save(new Store("부산점", "부산"));
        caUser = AuthenticatedUser.ca(1L, 2L, store.getId());
    }

    @Test
    void 구매_매장은_Principal에서_파생하고_같은_고객과_매장의_Visit을_연결한다() {
        Visit visit = visitRepository.save(new Visit(
            customer, store, LocalDateTime.of(2026, 8, 17, 10, 0)
        ));
        PurchaseCreateRequest request = request(
            customer.getId(), visit.getId(), 2, LocalDateTime.of(2026, 8, 17, 10, 30)
        );

        var response = service.createPurchase(caUser, request);

        assertThat(response.customerId()).isEqualTo(customer.getId());
        assertThat(response.storeId()).isEqualTo(store.getId());
        assertThat(response.visitId()).isEqualTo(visit.getId());
        assertThat(response.quantity()).isEqualTo(2);
    }

    @Test
    void Visit_고객이_Request_고객과_다르면_VISIT_CUSTOMER_MISMATCH다() {
        Visit visit = visitRepository.save(new Visit(
            otherCustomer, store, LocalDateTime.of(2026, 8, 17, 10, 0)
        ));

        assertThatThrownBy(() -> service.createPurchase(
            caUser,
            request(customer.getId(), visit.getId(), 1, LocalDateTime.of(2026, 8, 17, 10, 30))
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_CUSTOMER_MISMATCH);
    }

    @Test
    void 타_매장_Visit은_존재하지_않는_것처럼_차단한다() {
        Visit visit = visitRepository.save(new Visit(
            customer, otherStore, LocalDateTime.of(2026, 8, 17, 10, 0)
        ));

        assertThatThrownBy(() -> service.createPurchase(
            caUser,
            request(customer.getId(), visit.getId(), 1, LocalDateTime.of(2026, 8, 17, 10, 30))
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
    }

    @Test
    void Visit_없이도_구매를_생성할_수_있고_수량은_1_이상이어야_한다() {
        var response = service.createPurchase(
            caUser,
            request(customer.getId(), null, 1, LocalDateTime.of(2026, 8, 17, 10, 30))
        );
        assertThat(response.visitId()).isNull();

        assertThatThrownBy(() -> service.createPurchase(
            caUser,
            request(customer.getId(), null, 0, LocalDateTime.of(2026, 8, 17, 11, 0))
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void 고객별_구매_조회는_현재_CA_매장만_시간과_ID_내림차순으로_페이지화한다() {
        PurchaseHistory older = savePurchase(store, LocalDateTime.of(2026, 8, 15, 10, 0));
        PurchaseHistory newerFirst = savePurchase(store, LocalDateTime.of(2026, 8, 16, 10, 0));
        PurchaseHistory newerSecond = savePurchase(store, LocalDateTime.of(2026, 8, 16, 10, 0));
        savePurchase(otherStore, LocalDateTime.of(2026, 8, 17, 10, 0));

        var page = service.getCustomerPurchases(caUser, customer.getId(), 0, 20);

        assertThat(page.items()).extracting("purchaseId")
            .containsExactly(newerSecond.getId(), newerFirst.getId(), older.getId());
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void 구매_목록_페이지_범위를_검증한다() {
        assertThatThrownBy(() -> service.getCustomerPurchases(caUser, customer.getId(), 0, 0))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private PurchaseCreateRequest request(
        Long customerId,
        Long visitId,
        Integer quantity,
        LocalDateTime purchasedAt
    ) {
        return new PurchaseCreateRequest(
            customerId, product.getId(), visitId, quantity, purchasedAt
        );
    }

    private PurchaseHistory savePurchase(Store purchaseStore, LocalDateTime purchasedAt) {
        return purchaseRepository.saveAndFlush(new PurchaseHistory(
            customer, product, purchaseStore, null, 1, purchasedAt
        ));
    }

    private Customer saveCustomer(String loginId, String customerNo, String phone, String qrToken) {
        CustomerAccount account = customerAccountRepository.save(
            new CustomerAccount(loginId, passwordEncoder.encode("password123!"))
        );
        return customerRepository.save(new Customer(
            account,
            customerNo,
            "구매 고객",
            phone,
            null,
            "GOLD",
            qrToken,
            null,
            LocalDateTime.of(2026, 8, 1, 10, 0)
        ));
    }
}
