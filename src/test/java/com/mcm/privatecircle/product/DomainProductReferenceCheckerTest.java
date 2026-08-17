package com.mcm.privatecircle.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.product.service.DomainProductReferenceChecker;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;

import org.junit.jupiter.api.Test;

class DomainProductReferenceCheckerTest {

    private final CustomerInterestProductRepository interestRepository =
        mock(CustomerInterestProductRepository.class);
    private final PurchaseHistoryRepository purchaseRepository = mock(PurchaseHistoryRepository.class);
    private final DomainProductReferenceChecker checker = new DomainProductReferenceChecker(
        interestRepository, purchaseRepository
    );

    @Test
    void 관심상품_참조가_있으면_구매_조회_없이_사용중이다() {
        when(interestRepository.existsByProductId(10L)).thenReturn(true);

        assertThat(checker.isProductInUse(10L)).isTrue();
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void 관심상품이_없어도_구매_참조가_있으면_사용중이다() {
        when(interestRepository.existsByProductId(10L)).thenReturn(false);
        when(purchaseRepository.existsByProductId(10L)).thenReturn(true);

        assertThat(checker.isProductInUse(10L)).isTrue();
        verify(purchaseRepository).existsByProductId(10L);
    }
}
