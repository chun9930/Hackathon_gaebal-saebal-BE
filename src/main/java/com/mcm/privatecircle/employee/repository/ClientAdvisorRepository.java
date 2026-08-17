package com.mcm.privatecircle.employee.repository;

import java.util.Optional;

import com.mcm.privatecircle.employee.entity.ClientAdvisor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientAdvisorRepository extends JpaRepository<ClientAdvisor, Long> {

	Optional<ClientAdvisor> findByEmployeeAccountId(Long employeeAccountId);

	Optional<ClientAdvisor> findByEmployeeAccountIdAndStoreId(Long employeeAccountId, Long storeId);
}
