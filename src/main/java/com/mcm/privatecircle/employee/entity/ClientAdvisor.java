package com.mcm.privatecircle.employee.entity;

import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.global.common.entity.BaseTimeEntity;
import com.mcm.privatecircle.store.entity.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_advisors")
public class ClientAdvisor extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_account_id", nullable = false, unique = true)
	private EmployeeAccount employeeAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@Column(nullable = false, length = 100)
	private String name;

	protected ClientAdvisor() {
	}

	public ClientAdvisor(EmployeeAccount employeeAccount, Store store, String name) {
		this.employeeAccount = employeeAccount;
		this.store = store;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public EmployeeAccount getEmployeeAccount() {
		return employeeAccount;
	}

	public Store getStore() {
		return store;
	}

	public String getName() {
		return name;
	}

	public void updateName(String name) {
		if (name != null) {
			this.name = name;
		}
	}
}
