package com.mcm.privatecircle.mockdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mcm_private_circle_mock_seed;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.mock-data.seed-enabled=true",
    "app.mock-data.stores-resource=mock-data/test-stores.json",
    "app.mock-data.products-resource=mock-data/test-products.json"
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
}