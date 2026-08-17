package com.mcm.privatecircle.interest.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.entity.InterestSourceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerInterestProductRepository
    extends JpaRepository<CustomerInterestProduct, Long> {

    boolean existsByCustomerIdAndProductIdAndSourceType(
        Long customerId,
        Long productId,
        InterestSourceType sourceType
    );

    boolean existsByVisitRecordIdAndProductId(Long visitRecordId, Long productId);

    boolean existsByProductId(Long productId);

    @EntityGraph(attributePaths = "product")
    Page<CustomerInterestProduct> findByCustomerIdAndSourceType(
        Long customerId,
        InterestSourceType sourceType,
        Pageable pageable
    );

    @Query(
        value = """
            select interest
            from CustomerInterestProduct interest
            join fetch interest.product product
            left join interest.visitRecord visitRecord
            left join visitRecord.visit visit
            where interest.customer.id = :customerId
              and (
                interest.sourceType = :customerSource
                or (
                  interest.sourceType = :caSource
                  and visit.store.id = :storeId
                )
              )
            """,
        countQuery = """
            select count(interest)
            from CustomerInterestProduct interest
            left join interest.visitRecord visitRecord
            left join visitRecord.visit visit
            where interest.customer.id = :customerId
              and (
                interest.sourceType = :customerSource
                or (
                  interest.sourceType = :caSource
                  and visit.store.id = :storeId
                )
              )
            """
    )
    Page<CustomerInterestProduct> findVisibleToCa(
        @Param("customerId") Long customerId,
        @Param("storeId") Long storeId,
        @Param("customerSource") InterestSourceType customerSource,
        @Param("caSource") InterestSourceType caSource,
        Pageable pageable
    );

    @Query(
        """
            select interest
            from CustomerInterestProduct interest
            join fetch interest.product product
            left join fetch interest.visitRecord visitRecord
            left join fetch visitRecord.visit visit
            where interest.customer.id = :customerId
              and interest.savedAt < :visitedAt
              and (
                interest.sourceType = :customerSource
                or (
                  interest.sourceType = :caSource
                  and visit.store.id = :storeId
                  and visit.visitedAt < :visitedAt
                )
              )
            """
    )
    List<CustomerInterestProduct> findAiSourceInterests(
        @Param("customerId") Long customerId,
        @Param("storeId") Long storeId,
        @Param("visitedAt") LocalDateTime visitedAt,
        @Param("customerSource") InterestSourceType customerSource,
        @Param("caSource") InterestSourceType caSource,
        Pageable pageable
    );
}
