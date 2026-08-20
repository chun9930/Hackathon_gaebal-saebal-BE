package com.mcm.privatecircle.stamp.service;

import java.time.Clock;
import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ConstraintNameResolver;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.global.util.PaginationValidator;
import com.mcm.privatecircle.stamp.dto.VisitStampCreateRequest;
import com.mcm.privatecircle.stamp.dto.VisitStampResponse;
import com.mcm.privatecircle.stamp.entity.VisitStamp;
import com.mcm.privatecircle.stamp.repository.VisitStampRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class VisitStampService {

    private static final Logger log = LoggerFactory.getLogger(VisitStampService.class);
    private static final String VISIT_UNIQUE_CONSTRAINT = "uk_visit_stamps_visit";
    private static final Sort STAMP_SORT = Sort.by(
        Sort.Order.desc("issuedAt"),
        Sort.Order.desc("id")
    );

    private final VisitStampRepository stampRepository;
    private final VisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    private final ClientAdvisorRepository clientAdvisorRepository;
    private final Clock clock;

    public VisitStampService(
        VisitStampRepository stampRepository,
        VisitRepository visitRepository,
        CustomerRepository customerRepository,
        ClientAdvisorRepository clientAdvisorRepository,
        Clock clock
    ) {
        this.stampRepository = stampRepository;
        this.visitRepository = visitRepository;
        this.customerRepository = customerRepository;
        this.clientAdvisorRepository = clientAdvisorRepository;
        this.clock = clock;
    }

    @Transactional
    public VisitStampResponse issueStamp(
        AuthenticatedUser authenticatedUser,
        Long visitId,
        VisitStampCreateRequest request
    ) {
        log.info(
            "[STAMP] issue requested: visitId={}, caId={}, storeId={}",
            visitId,
            authenticatedUser == null ? null : authenticatedUser.getCaId(),
            authenticatedUser == null ? null : authenticatedUser.getStoreId()
        );
        requireCa(authenticatedUser);
        validateRequest(request);
        Visit visit = visitRepository.findByIdAndStoreId(visitId, authenticatedUser.getStoreId())
            .orElseThrow(() -> {
                log.warn(
                    "[STAMP] visit not found in CA store: visitId={}, caId={}, storeId={}",
                    visitId,
                    authenticatedUser.getCaId(),
                    authenticatedUser.getStoreId()
                );
                return new BusinessException(ErrorCode.VISIT_NOT_FOUND);
            });
        ClientAdvisor ca = findAuthenticatedCa(authenticatedUser);
        Long customerId = visit.getCustomer().getId();
        log.info(
            "[STAMP] issue target resolved: visitId={}, customerId={}, caId={}, storeId={}",
            visitId,
            customerId,
            ca.getId(),
            authenticatedUser.getStoreId()
        );
        if (stampRepository.existsByVisitId(visitId)) {
            log.warn(
                "[STAMP] duplicate issue blocked: visitId={}, customerId={}, caId={}",
                visitId,
                customerId,
                ca.getId()
            );
            throw new BusinessException(ErrorCode.STAMP_ALREADY_ISSUED);
        }

        VisitStamp stamp = new VisitStamp(
            visit,
            visit.getCustomer(),
            ca,
            request.stampType(),
            LocalDateTime.now(clock)
        );
        try {
            VisitStamp savedStamp = stampRepository.saveAndFlush(stamp);
            Long stampId = savedStamp.getId();
            log.info(
                "[STAMP] database flush succeeded: stampId={}, visitId={}, customerId={}, caId={}, issuedAt={}",
                stampId,
                visitId,
                customerId,
                ca.getId(),
                savedStamp.getIssuedAt()
            );
            logAfterCommit(stampId, visitId, customerId, ca.getId());
            return VisitStampResponse.from(savedStamp);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintNameResolver.contains(exception, VISIT_UNIQUE_CONSTRAINT)) {
                log.warn(
                    "[STAMP] duplicate issue rejected by database: visitId={}, customerId={}, caId={}",
                    visitId,
                    customerId,
                    ca.getId()
                );
                throw new BusinessException(ErrorCode.STAMP_ALREADY_ISSUED);
            }
            log.error(
                "[STAMP] database save failed: visitId={}, customerId={}, caId={}",
                visitId,
                customerId,
                ca.getId(),
                exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitStampResponse> getCustomerStamps(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        int page,
        int size
    ) {
        requireCa(authenticatedUser);
        PaginationValidator.validate(page, size);
        requireCustomerExists(customerId);
        var stamps = stampRepository.findByCustomerIdAndVisitStoreId(
            customerId,
            authenticatedUser.getStoreId(),
            PageRequest.of(page, size, STAMP_SORT)
        );
        log.info(
            "[STAMP] CA customer stamps loaded: customerId={}, caId={}, storeId={}, totalElements={}",
            customerId,
            authenticatedUser.getCaId(),
            authenticatedUser.getStoreId(),
            stamps.getTotalElements()
        );
        return PageResponse.from(stamps.map(VisitStampResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitStampResponse> getMyStamps(
        AuthenticatedUser authenticatedUser,
        int page,
        int size
    ) {
        requireCustomer(authenticatedUser);
        PaginationValidator.validate(page, size);
        requireCustomerExists(authenticatedUser.getCustomerId());
        var stamps = stampRepository.findByCustomerId(
            authenticatedUser.getCustomerId(),
            PageRequest.of(page, size, STAMP_SORT)
        );
        log.info(
            "[STAMP] customer stamps loaded: customerId={}, totalElements={}",
            authenticatedUser.getCustomerId(),
            stamps.getTotalElements()
        );
        return PageResponse.from(stamps.map(VisitStampResponse::from));
    }

    private void logAfterCommit(Long stampId, Long visitId, Long customerId, Long caId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info(
                    "[STAMP] transaction committed: stampId={}, visitId={}, customerId={}, caId={}",
                    stampId,
                    visitId,
                    customerId,
                    caId
                );
            }
        });
    }

    private ClientAdvisor findAuthenticatedCa(AuthenticatedUser authenticatedUser) {
        ClientAdvisor ca = clientAdvisorRepository.findById(authenticatedUser.getCaId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CA_NOT_FOUND));
        if (!ca.getStore().getId().equals(authenticatedUser.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_CA);
        }
        return ca;
    }

    private void requireCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    private void validateRequest(VisitStampCreateRequest request) {
        if (request == null
            || request.stampType() == null
            || request.stampType().isBlank()
            || request.stampType().length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
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
