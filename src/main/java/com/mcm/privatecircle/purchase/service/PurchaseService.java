package com.mcm.privatecircle.purchase.service;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.purchase.dto.PurchaseCreateRequest;
import com.mcm.privatecircle.purchase.dto.PurchaseResponse;
import com.mcm.privatecircle.purchase.entity.PurchaseHistory;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private static final Sort PURCHASE_SORT = Sort.by(
        Sort.Order.desc("purchasedAt"),
        Sort.Order.desc("id")
    );

    private final PurchaseHistoryRepository purchaseRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final VisitRepository visitRepository;

    public PurchaseService(
        PurchaseHistoryRepository purchaseRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        StoreRepository storeRepository,
        VisitRepository visitRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public PurchaseResponse createPurchase(
        AuthenticatedUser authenticatedUser,
        PurchaseCreateRequest request
    ) {
        requireCa(authenticatedUser);
        validateRequest(request);
        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        Store store = storeRepository.findById(authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        Visit visit = findAndValidateVisit(request.visitId(), customer.getId(), store.getId());

        PurchaseHistory purchase = purchaseRepository.save(new PurchaseHistory(
            customer,
            product,
            store,
            visit,
            request.quantity(),
            request.purchasedAt()
        ));
        return PurchaseResponse.from(purchase);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponse> getCustomerPurchases(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        int page,
        int size
    ) {
        requireCa(authenticatedUser);
        PaginationValidator.validate(page, size);
        if (!customerRepository.existsById(customerId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        var purchases = purchaseRepository.findByCustomerIdAndStoreId(
            customerId,
            authenticatedUser.getStoreId(),
            PageRequest.of(page, size, PURCHASE_SORT)
        );
        return PageResponse.from(purchases.map(PurchaseResponse::from));
    }

    private Visit findAndValidateVisit(Long visitId, Long customerId, Long storeId) {
        if (visitId == null) {
            return null;
        }
        Visit visit = visitRepository.findByIdAndStoreId(visitId, storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        if (!visit.belongsToCustomer(customerId)) {
            throw new BusinessException(ErrorCode.VISIT_CUSTOMER_MISMATCH);
        }
        return visit;
    }

    private void validateRequest(PurchaseCreateRequest request) {
        if (request == null
            || request.customerId() == null
            || request.productId() == null
            || request.quantity() == null
            || request.quantity() < 1
            || request.purchasedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
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
