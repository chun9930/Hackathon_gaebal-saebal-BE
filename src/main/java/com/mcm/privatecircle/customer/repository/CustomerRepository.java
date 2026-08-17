package com.mcm.privatecircle.customer.repository;

import java.util.Optional;

import com.mcm.privatecircle.customer.entity.Customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByCustomerAccountId(Long customerAccountId);

	Optional<Customer> findByPhoneNumber(String phoneNumber);

	Optional<Customer> findByQrToken(String qrToken);

	boolean existsByPhoneNumber(String phoneNumber);

	boolean existsByQrToken(String qrToken);
}
