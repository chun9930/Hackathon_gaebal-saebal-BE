package com.mcm.privatecircle.mockdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.product.dto.ProductSummaryResponse;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.product.service.ProductService;
import com.mcm.privatecircle.store.dto.StoreSummaryResponse;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.store.service.StoreService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcm_private_circle_mock_seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.mock-data.seed-enabled=true",
    "app.mock-data.stores-resource=mock-data/test-stores.json",
    "app.mock-data.products-resource=mock-data/test-products.json",
    "app.mock-data.employee-seed-enabled=true",
    "app.mock-data.employees[0].login-id=CA-TEST-A",
    "app.mock-data.employees[0].password=1001",
    "app.mock-data.employees[0].name=Test Advisor A",
    "app.mock-data.employees[0].store-name=Mock Store Alpha",
    "app.mock-data.employees[1].login-id=CA-TEST-B",
    "app.mock-data.employees[1].password=1002",
    "app.mock-data.employees[1].name=Test Advisor B",
    "app.mock-data.employees[1].store-name=Mock Store Beta"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MockDataSeederIntegrationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private EmployeeAccountRepository employeeAccountRepository;

    @Autowired
    private ClientAdvisorRepository clientAdvisorRepository;

    @Test
    void seedsStoresAndProductsWhenEnabled() {
        assertThat(storeRepository.count()).isEqualTo(2);
        assertThat(productRepository.count()).isEqualTo(2);
        assertThat(productRepository.findByProductCode("mock-bag-001")).isPresent();
        assertThat(productRepository.findByProductCode("mock-acc-001")).isPresent();
    }

    @Test
    void seededDataIsImmediatelyVisibleThroughExistingStoreAndProductServices() {
        List<StoreSummaryResponse> stores = storeService.getStores();
        List<ProductSummaryResponse> products = productService.getProducts();

        assertThat(stores).hasSize(2);
        assertThat(stores)
            .extracting(StoreSummaryResponse::name)
            .containsExactly("Mock Store Alpha", "Mock Store Beta");

        assertThat(products).hasSize(2);
        assertThat(products)
            .extracting(ProductSummaryResponse::productCode)
            .containsExactlyInAnyOrder("mock-bag-001", "mock-acc-001");
    }

    @Test
    @Transactional
    void seedsDevelopmentAdvisorsForConfiguredStores() {
        var accountA = employeeAccountRepository.findByLoginId("CA-TEST-A").orElseThrow();
        var accountB = employeeAccountRepository.findByLoginId("CA-TEST-B").orElseThrow();
        var advisorA = clientAdvisorRepository.findByEmployeeAccountId(accountA.getId()).orElseThrow();
        var advisorB = clientAdvisorRepository.findByEmployeeAccountId(accountB.getId()).orElseThrow();

        assertThat(advisorA.getStore().getName()).isEqualTo("Mock Store Alpha");
        assertThat(advisorB.getStore().getName()).isEqualTo("Mock Store Beta");
    }
}
