package com.mcm.privatecircle.customer.repository;

import java.util.Optional;

import com.mcm.privatecircle.customer.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByCustomerAccountId(Long customerAccountId);

	Optional<Customer> findByPhoneNumber(String phoneNumber);

	Optional<Customer> findByQrToken(String qrToken);

	boolean existsByPhoneNumber(String phoneNumber);

	boolean existsByQrToken(String qrToken);

    @Query("""
        select c
        from Customer c
        where lower(c.name) like lower(concat('%', :keyword, '%'))
           or c.phoneNumber like concat('%', :keyword, '%')
           or lower(coalesce(c.customerNo, '')) like lower(concat('%', :keyword, '%'))
        """)
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}