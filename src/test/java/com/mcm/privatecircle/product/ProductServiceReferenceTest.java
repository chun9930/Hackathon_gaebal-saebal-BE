package com.mcm.privatecircle.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.product.service.ProductReferenceChecker;
import com.mcm.privatecircle.product.service.ProductService;

import org.junit.jupiter.api.Test;

class ProductServiceReferenceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductReferenceChecker referenceChecker = mock(ProductReferenceChecker.class);
    private final ProductService service = new ProductService(productRepository, referenceChecker);
    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @Test
    void 관심상품이나_구매에서_참조하는_상품은_삭제하지_않는다() {
        Product product = mock(Product.class);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(referenceChecker.isProductInUse(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteProduct(caUser, 10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PRODUCT_IN_USE);
        verify(productRepository, never()).delete(product);
    }

    @Test
    void 참조가_없는_상품은_기존_삭제_흐름을_유지한다() {
        Product product = mock(Product.class);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(referenceChecker.isProductInUse(10L)).thenReturn(false);

        service.deleteProduct(caUser, 10L);

        verify(productRepository).delete(product);
    }
}
