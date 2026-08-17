package com.mcm.privatecircle.global.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MockDataSeeder implements ApplicationRunner {

    private static final TypeReference<List<StoreSeedItem>> STORE_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<ProductSeedItem>> PRODUCT_LIST = new TypeReference<>() {
    };

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final MockDataProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public MockDataSeeder(
        StoreRepository storeRepository,
        ProductRepository productRepository,
        MockDataProperties properties
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.seedEnabled()) {
            return;
        }
        seedStoresIfEmpty();
        seedProductsIfEmpty();
    }

    private void seedStoresIfEmpty() throws IOException {
        if (storeRepository.count() > 0) {
            return;
        }
        List<StoreSeedItem> stores = readList(properties.storesResource(), STORE_LIST);
        storeRepository.saveAll(
            stores.stream()
                .map(item -> new Store(item.name(), item.location()))
                .toList()
        );
    }

    private void seedProductsIfEmpty() throws IOException {
        if (productRepository.count() > 0) {
            return;
        }
        List<ProductSeedItem> products = readList(properties.productsResource(), PRODUCT_LIST);
        productRepository.saveAll(
            products.stream()
                .map(item -> new Product(
                    item.productCode(),
                    item.name(),
                    item.category(),
                    item.imageUrl(),
                    item.price(),
                    item.dppId(),
                    item.recommendable() == null || item.recommendable()
                ))
                .toList()
        );
    }

    private <T> List<T> readList(String location, TypeReference<List<T>> typeReference) throws IOException {
        try (InputStream inputStream = new ClassPathResource(location).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }

    private record StoreSeedItem(
        String name,
        String location
    ) {
    }

    private record ProductSeedItem(
        String productCode,
        String name,
        String category,
        String imageUrl,
        BigDecimal price,
        String dppId,
        Boolean recommendable
    ) {
    }
}
