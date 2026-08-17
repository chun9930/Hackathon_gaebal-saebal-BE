package com.mcm.privatecircle.ai.service;

import com.mcm.privatecircle.ai.client.GeminiBriefClient;
import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.repository.AiJourneyBriefRepository;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiBriefService {

    public static final Sort HISTORY_SORT = Sort.by(
        Sort.Order.desc("generatedAt"),
        Sort.Order.desc("id")
    );

    private final AiJourneyBriefRepository repository;
    private final VisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    @SuppressWarnings("unused")
    private final GeminiBriefClient geminiBriefClient;

    public AiBriefService(
        AiJourneyBriefRepository repository,
        VisitRepository visitRepository,
        CustomerRepository customerRepository,
        GeminiBriefClient geminiBriefClient
    ) {
        this.repository = repository;
        this.visitRepository = visitRepository;
        this.customerRepository = customerRepository;
        this.geminiBriefClient = geminiBriefClient;
    }

    @Transactional(readOnly = true)
    public AiBriefResponse getLatest(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        Long visitId
    ) {
        requireCa(authenticatedUser);
        Visit visit = visitRepository.findByIdAndStoreId(visitId, authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        if (!visit.belongsToCustomer(customerId)) {
            throw new BusinessException(ErrorCode.VISIT_CUSTOMER_MISMATCH);
        }
        return repository.findTopByCustomerIdAndVisitIdOrderByGeneratedAtDescIdDesc(customerId, visitId)
            .map(AiBriefResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.AI_BRIEF_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResponse<AiBriefResponse> getHistory(
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
        return PageResponse.from(
            repository.findByCustomerIdAndVisitStoreId(
                customerId,
                authenticatedUser.getStoreId(),
                PageRequest.of(page, size, HISTORY_SORT)
            ).map(AiBriefResponse::from)
        );
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
