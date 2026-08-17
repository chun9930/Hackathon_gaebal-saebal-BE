package com.mcm.privatecircle.account.repository;

import java.util.Optional;

import com.mcm.privatecircle.account.entity.CustomerAccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {

	Optional<CustomerAccount> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);
}
