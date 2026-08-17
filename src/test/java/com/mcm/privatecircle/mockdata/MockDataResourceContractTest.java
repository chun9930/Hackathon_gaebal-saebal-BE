package com.mcm.privatecircle.mockdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MockDataResourceContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesResourceContainsNineteenEntries() throws Exception {
        List<StoreMockResource> stores = objectMapper.readValue(
            new ClassPathResource("mock-data/stores.json").getInputStream(),
            new TypeReference<List<StoreMockResource>>() {
            }
        );

        assertThat(stores).hasSize(19);
        assertThat(stores)
            .allSatisfy(store -> {
                assertThat(store.name()).isNotBlank();
                assertThat(store.location()).isNotBlank();
            });
    }

    @Test
    void productsResourceContainsNineteenEntriesWithUniqueCodesAndResolvableStaticImages() throws Exception {
        List<ProductMockResource> products = objectMapper.readValue(
            new ClassPathResource("mock-data/products.json").getInputStream(),
            new TypeReference<List<ProductMockResource>>() {
            }
        );

        assertThat(products).hasSize(19);

        Set<String> productCodes = products.stream()
            .map(ProductMockResource::productCode)
            .collect(Collectors.toSet());

        assertThat(productCodes).hasSize(19);

        for (ProductMockResource product : products) {
            assertThat(product.productCode()).isNotBlank();
            assertThat(product.name()).isNotBlank();
            assertThat(product.category()).isNotBlank();
            assertThat(product.price()).isPositive();
            assertThat(product.imageUrl()).startsWith("/");
            assertThat(new ClassPathResource("static" + product.imageUrl()).exists())
                .as("missing static image for productCode=%s, path=%s", product.productCode(), product.imageUrl())
                .isTrue();
        }
    }

    private record StoreMockResource(
        String name,
        String location
    ) {
    }

    private record ProductMockResource(
        String productCode,
        String name,
        String category,
        Integer price,
        String imageUrl,
        Boolean recommendable
    ) {
    }
}
