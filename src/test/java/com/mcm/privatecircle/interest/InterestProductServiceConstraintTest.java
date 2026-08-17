package com.mcm.privatecircle.interest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.interest.dto.CaInterestCreateRequest;
import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.interest.service.InterestProductService;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class InterestProductServiceConstraintTest {

    private final CustomerInterestProductRepository interestRepository =
        mock(CustomerInterestProductRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final VisitRecordRepository visitRecordRepository = mock(VisitRecordRepository.class);
    private final InterestProductService service = new InterestProductService(
        interestRepository,
        customerRepository,
        productRepository,
        visitRecordRepository,
        Clock.system(ZoneId.of("Asia/Seoul"))
    );

    private final AuthenticatedUser caUser = AuthenticatedUser.ca(1L, 2L, 3L);

    @BeforeEach
    void setUp() {
        Customer customer = mock(Customer.class);
        Product product = mock(Product.class);
        VisitRecord visitRecord = mock(VisitRecord.class);
        when(customer.getId()).thenReturn(10L);
        when(product.getId()).thenReturn(20L);
        when(visitRecord.getId()).thenReturn(30L);
        when(visitRecord.getCustomer()).thenReturn(customer);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product));
        when(visitRecordRepository.findByIdAndVisitStoreId(30L, 3L))
            .thenReturn(Optional.of(visitRecord));
        when(interestRepository.existsByVisitRecordIdAndProductId(30L, 20L)).thenReturn(false);
    }

    @Test
    void 알려진_CA_Unique_충돌만_중복_409로_변환한다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "저장 실패",
            new RuntimeException("uk_interest_visit_record_product")
        );
        when(interestRepository.saveAndFlush(any(CustomerInterestProduct.class)))
            .thenThrow(exception);

        assertThatThrownBy(() -> service.createCaInterest(caUser, 10L, request()))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
    }

    @Test
    void 알_수_없는_DB_오류는_중복으로_변환하지_않는다() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "unknown_constraint"
        );
        when(interestRepository.saveAndFlush(any(CustomerInterestProduct.class)))
            .thenThrow(exception);

        assertThatThrownBy(() -> service.createCaInterest(caUser, 10L, request()))
            .isSameAs(exception);
    }

    private CaInterestCreateRequest request() {
        return new CaInterestCreateRequest(20L, 30L, "CA 관심");
    }
}
