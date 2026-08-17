package com.mcm.privatecircle.interest.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ConstraintNameResolver;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.interest.dto.CaInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.CustomerInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.InterestProductResponse;
import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.entity.InterestSourceType;
import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterestProductService {

    private static final String CA_UNIQUE_CONSTRAINT = "uk_interest_visit_record_product";
    private static final Sort INTEREST_SORT = Sort.by(
        Sort.Order.desc("savedAt"),
        Sort.Order.desc("id")
    );

    private final CustomerInterestProductRepository interestRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final Clock clock;

    public InterestProductService(
        CustomerInterestProductRepository interestRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        VisitRecordRepository visitRecordRepository,
        Clock clock
    ) {
        this.interestRepository = interestRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.clock = clock;
    }

    @Transactional
    public InterestProductResponse createCustomerInterest(
        AuthenticatedUser authenticatedUser,
        CustomerInterestCreateRequest request
    ) {
        requireCustomer(authenticatedUser);
        Customer customer = findCustomer(authenticatedUser.getCustomerId());
        Product product = findProduct(request.productId());
        if (interestRepository.existsByCustomerIdAndProductIdAndSourceType(
            customer.getId(), product.getId(), InterestSourceType.CUSTOMER
        )) {
            throw new BusinessException(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
        }

        CustomerInterestProduct interest = new CustomerInterestProduct(
            customer,
            product,
            InterestSourceType.CUSTOMER,
            null,
            request.memo(),
            LocalDateTime.now(clock)
        );
        return InterestProductResponse.from(interestRepository.save(interest));
    }

    @Transactional
    public InterestProductResponse createCaInterest(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        CaInterestCreateRequest request
    ) {
        requireCa(authenticatedUser);
        Customer customer = findCustomer(customerId);
        Product product = findProduct(request.productId());
        VisitRecord visitRecord = visitRecordRepository
            .findByIdAndVisitStoreId(request.visitRecordId(), authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_RECORD_NOT_FOUND));

        if (!visitRecord.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(ErrorCode.INVALID_INTEREST_SOURCE);
        }
        if (interestRepository.existsByVisitRecordIdAndProductId(visitRecord.getId(), product.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
        }

        CustomerInterestProduct interest = new CustomerInterestProduct(
            customer,
            product,
            InterestSourceType.CA,
            visitRecord,
            request.memo(),
            LocalDateTime.now(clock)
        );
        try {
            return InterestProductResponse.from(interestRepository.saveAndFlush(interest));
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintNameResolver.contains(exception, CA_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.DUPLICATE_INTEREST_PRODUCT);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<InterestProductResponse> getMyInterestProducts(
        AuthenticatedUser authenticatedUser,
        int page,
        int size
    ) {
        requireCustomer(authenticatedUser);
        PaginationValidator.validate(page, size);
        findCustomer(authenticatedUser.getCustomerId());
        var interests = interestRepository.findByCustomerIdAndSourceType(
            authenticatedUser.getCustomerId(),
            InterestSourceType.CUSTOMER,
            PageRequest.of(page, size, INTEREST_SORT)
        );
        return PageResponse.from(interests.map(InterestProductResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<InterestProductResponse> getCustomerInterestProducts(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        int page,
        int size
    ) {
        requireCa(authenticatedUser);
        PaginationValidator.validate(page, size);
        findCustomer(customerId);
        var interests = interestRepository.findVisibleToCa(
            customerId,
            authenticatedUser.getStoreId(),
            InterestSourceType.CUSTOMER,
            InterestSourceType.CA,
            PageRequest.of(page, size, INTEREST_SORT)
        );
        return PageResponse.from(interests.map(InterestProductResponse::from));
    }

    @Transactional
    public void deleteInterestProduct(
        AuthenticatedUser authenticatedUser,
        Long interestProductId
    ) {
        CustomerInterestProduct interest = interestRepository.findById(interestProductId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INTEREST_PRODUCT_NOT_FOUND));

        if (authenticatedUser != null && authenticatedUser.getRole() == UserRole.CUSTOMER) {
            requireCustomer(authenticatedUser);
            if (!interest.isCustomerSourceOwnedBy(authenticatedUser.getCustomerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (authenticatedUser != null && authenticatedUser.getRole() == UserRole.CA) {
            requireCa(authenticatedUser);
            if (!interest.isCaSourceOwnedBy(
                authenticatedUser.getCaId(), authenticatedUser.getStoreId()
            )) {
                throw new BusinessException(ErrorCode.FORBIDDEN_CA);
            }
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        interestRepository.delete(interest);
        interestRepository.flush();
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void requireCustomer(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null
            || authenticatedUser.getRole() != UserRole.CUSTOMER
            || authenticatedUser.getCustomerId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireCa(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null
            || authenticatedUser.getRole() != UserRole.CA
            || authenticatedUser.getCaId() == null
            || authenticatedUser.getStoreId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
