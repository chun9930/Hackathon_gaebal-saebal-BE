package com.mcm.privatecircle.visit.service;

import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.visit.dto.VisitCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitResponse;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private static final Sort VISIT_SORT = Sort.by(
        Sort.Order.desc("visitedAt"),
        Sort.Order.desc("id")
    );

    private final VisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    public VisitService(
        VisitRepository visitRepository,
        CustomerRepository customerRepository,
        StoreRepository storeRepository
    ) {
        this.visitRepository = visitRepository;
        this.customerRepository = customerRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public VisitResponse createVisit(AuthenticatedUser authenticatedUser, VisitCreateRequest request) {
        requireCa(authenticatedUser);
        Customer customer = findCustomer(request.customerId());
        Store store = storeRepository.findById(authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        Visit visit = visitRepository.save(new Visit(customer, store, request.visitedAt()));
        return VisitResponse.from(visit);
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> getCustomerVisits(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        int page,
        int size
    ) {
        requireCa(authenticatedUser);
        PaginationValidator.validate(page, size);
        findCustomer(customerId);

        var visits = visitRepository.findByCustomerIdAndStoreId(
            customerId,
            authenticatedUser.getStoreId(),
            PageRequest.of(page, size, VISIT_SORT)
        );
        return PageResponse.from(visits.map(VisitResponse::from));
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisit(AuthenticatedUser authenticatedUser, Long visitId) {
        requireCa(authenticatedUser);
        Visit visit = visitRepository.findByIdAndStoreId(visitId, authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        return VisitResponse.from(visit);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
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
