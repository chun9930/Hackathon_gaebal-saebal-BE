package com.mcm.privatecircle.ai.service;

import java.time.LocalDateTime;

import com.mcm.privatecircle.ai.dto.AiBriefSource;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.interest.entity.InterestSourceType;
import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;
import com.mcm.privatecircle.visit.entity.Visit;
import com.mcm.privatecircle.visit.entity.VisitRecord;
import com.mcm.privatecircle.visit.repository.VisitRecordRepository;
import com.mcm.privatecircle.visit.repository.VisitRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiBriefSourceReader {

    private static final Sort VISIT_RECORD_SORT = Sort.by(
        Sort.Order.desc("visit.visitedAt"),
        Sort.Order.desc("id")
    );
    private static final Sort INTEREST_SORT = Sort.by(
        Sort.Order.desc("savedAt"),
        Sort.Order.desc("id")
    );
    private static final Sort PURCHASE_SORT = Sort.by(
        Sort.Order.desc("purchasedAt"),
        Sort.Order.desc("id")
    );

    private final VisitRepository visitRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final CustomerInterestProductRepository interestRepository;
    private final PurchaseHistoryRepository purchaseRepository;

    public AiBriefSourceReader(
        VisitRepository visitRepository,
        VisitRecordRepository visitRecordRepository,
        CustomerInterestProductRepository interestRepository,
        PurchaseHistoryRepository purchaseRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitRecordRepository = visitRecordRepository;
        this.interestRepository = interestRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public AiBriefSource read(
        AuthenticatedUser authenticatedUser,
        Long customerId,
        Long visitId
    ) {
        requireCa(authenticatedUser);
        Visit targetVisit = visitRepository.findByIdAndStoreId(visitId, authenticatedUser.getStoreId())
            .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
        if (!targetVisit.belongsToCustomer(customerId)) {
            throw new BusinessException(ErrorCode.VISIT_CUSTOMER_MISMATCH);
        }

        LocalDateTime targetVisitedAt = targetVisit.getVisitedAt();
        VisitRecord currentVisitRecord = visitRecordRepository.findByVisitId(visitId).orElse(null);
        var visitRecords = visitRecordRepository.findByCustomerIdAndVisitStoreIdAndVisitVisitedAtLessThan(
            customerId,
            authenticatedUser.getStoreId(),
            targetVisitedAt,
            PageRequest.of(0, 5, VISIT_RECORD_SORT)
        );
        var interests = interestRepository.findAiSourceInterests(
            customerId,
            authenticatedUser.getStoreId(),
            targetVisitedAt,
            InterestSourceType.CUSTOMER,
            InterestSourceType.CA,
            PageRequest.of(0, 10, INTEREST_SORT)
        );
        var purchases = purchaseRepository.findByCustomerIdAndStoreIdAndPurchasedAtLessThan(
            customerId,
            authenticatedUser.getStoreId(),
            targetVisitedAt,
            PageRequest.of(0, 10, PURCHASE_SORT)
        );

        return new AiBriefSource(
            new AiBriefSource.CustomerProfile(
                targetVisit.getCustomer().getMembershipGrade(),
                targetVisit.getCustomer().getStylePreferences()
            ),
            currentVisitRecord == null
                ? null
                : new AiBriefSource.VisitRecordSource(
                    currentVisitRecord.getVisit().getVisitedAt(),
                    currentVisitRecord.getVisitPurpose(),
                    currentVisitRecord.getContent(),
                    currentVisitRecord.getStyleChangeNote(),
                    currentVisitRecord.getCautionNote()
                ),
            visitRecords.stream()
                .map(record -> new AiBriefSource.VisitRecordSource(
                    record.getVisit().getVisitedAt(),
                    record.getVisitPurpose(),
                    record.getContent(),
                    record.getStyleChangeNote(),
                    record.getCautionNote()
                ))
                .toList(),
            interests.stream()
                .map(interest -> new AiBriefSource.InterestProductSource(
                    interest.getProduct().getName(),
                    interest.getProduct().getCategory(),
                    interest.getSourceType(),
                    interest.getMemo(),
                    interest.getSavedAt()
                ))
                .toList(),
            purchases.stream()
                .map(purchase -> new AiBriefSource.PurchaseSource(
                    purchase.getProduct().getName(),
                    purchase.getProduct().getCategory(),
                    purchase.getQuantity(),
                    purchase.getPurchasedAt()
                ))
                .toList(),
            visitRecords.size()
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
