package com.mcm.privatecircle.account.repository;

import java.util.Optional;

import com.mcm.privatecircle.account.entity.EmployeeAccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeAccountRepository extends JpaRepository<EmployeeAccount, Long> {

	Optional<EmployeeAccount> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);
}
