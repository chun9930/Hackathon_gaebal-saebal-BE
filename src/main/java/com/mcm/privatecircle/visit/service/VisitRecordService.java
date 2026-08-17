package com.mcm.privatecircle.visit.service;

import com.mcm.privatecircle.employee.entity.ClientAdvisor;
import com.mcm.privatecircle.employee.repository.ClientAdvisorRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ConstraintNameResolver;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.visit.dto.VisitRecordCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitRecordResponse;
import com.mcm.privatecircle.visit.dto.VisitRecordUpdateRequest;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitRecordService {

    private static final String VISIT_UNIQUE_CONSTRAINT = "uk_visit_records_visit";

    private final VisitRecordRepository visitRecordRepository;
    private final VisitRepository visitRepository;
    private final ClientAdvisorRepository clientAdvisorRepository;

    public VisitRecordService(
        VisitRecordRepository visitRecordRepository,
        VisitRepository visitRepository,
        ClientAdvisorRepository clientAdvisorRepository
    ) {
        this.visitRecordRepository = visitRecordRepository;
        this.visitRepository = visitRepository;
        this.clientAdvisorRepository = clientAdvisorRepository;
    }

    @Transactional
    public VisitRecordResponse createVisitRecord(
        AuthenticatedUser authenticatedUser,
        Long visitId,
        VisitRecordCreateRequest request
    ) {
        requireCa(authenticatedUser);
        Visit visit = findVisitInStore(visitId, authenticatedUser.getStoreId());
        ClientAdvisor ca = findAuthenticatedCa(authenticatedUser);

        if (visitRecordRepository.existsByVisitId(visitId)) {
            throw new BusinessException(ErrorCode.VISIT_RECORD_ALREADY_EXISTS);
        }

        VisitRecord visitRecord = new VisitRecord(
            visit,
            visit.getCustomer(),
            ca,
            request.visitPurpose(),
            request.content(),
            request.styleChangeNote(),
            request.cautionNote()
        );

        try {
            return VisitRecordResponse.from(visitRecordRepository.saveAndFlush(visitRecord));
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintNameResolver.contains(exception, VISIT_UNIQUE_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.VISIT_RECORD_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public VisitRecordResponse getVisitRecord(AuthenticatedUser authenticatedUser, Long visitId) {
        requireCa(authenticatedUser);
        findVisitInStore(visitId, authenticatedUser.getStoreId());
        VisitRecord visitRecord = visitRecordRepository.findByVisitId(visitId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_RECORD_NOT_FOUND));
        return VisitRecordResponse.from(visitRecord);
    }

    @Transactional
    public VisitRecordResponse updateVisitRecord(
        AuthenticatedUser authenticatedUser,
        Long visitRecordId,
        VisitRecordUpdateRequest request
    ) {
        requireCa(authenticatedUser);
        if (!request.hasAnyField()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        VisitRecord visitRecord = visitRecordRepository
            .findByIdAndVisitStoreId(visitRecordId, authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_RECORD_NOT_FOUND));
        if (!visitRecord.isAuthoredBy(authenticatedUser.getCaId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_CA);
        }

        visitRecord.update(
            request.visitPurpose(),
            request.content(),
            request.styleChangeNote(),
            request.cautionNote()
        );
        return VisitRecordResponse.from(visitRecord);
    }

    private Visit findVisitInStore(Long visitId, Long storeId) {
        return visitRepository.findByIdAndStoreId(visitId, storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
    }

    private ClientAdvisor findAuthenticatedCa(AuthenticatedUser authenticatedUser) {
        ClientAdvisor ca = clientAdvisorRepository.findById(authenticatedUser.getCaId())
            .orElseThrow(() -> new BusinessException(ErrorCode.CA_NOT_FOUND));
        if (!ca.getStore().getId().equals(authenticatedUser.getStoreId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_CA);
        }
        return ca;
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
