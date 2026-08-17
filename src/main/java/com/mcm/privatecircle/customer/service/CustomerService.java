package com.mcm.privatecircle.customer.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.mcm.privatecircle.customer.dto.CustomerProfileResponse;
import com.mcm.privatecircle.customer.dto.CustomerProfileUpdateRequest;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final JdbcTemplate jdbcTemplate;

	public CustomerService(CustomerRepository customerRepository, JdbcTemplate jdbcTemplate) {
		this.customerRepository = customerRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	public CustomerProfileResponse getMyProfile(AuthenticatedUser authenticatedUser) {
		return buildProfile(
			customerRepository.findById(authenticatedUser.getCustomerId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND))
		);
	}

	public CustomerProfileResponse updateMyProfile(AuthenticatedUser authenticatedUser, CustomerProfileUpdateRequest request) {
		Customer customer = customerRepository.findById(authenticatedUser.getCustomerId())
			.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

		if (request.phoneNumber() != null) {
			customerRepository.findByPhoneNumber(request.phoneNumber())
				.filter(existing -> !existing.getId().equals(customer.getId()))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
				});
		}

		customer.updateProfile(
			request.name(),
			request.phoneNumber(),
			request.profileImageUrl(),
			request.membershipGrade(),
			request.stylePreferences()
		);
		return buildProfile(customerRepository.save(customer));
	}

	public CustomerProfileResponse getCustomerDetail(Long customerId) {
		return buildProfile(
			customerRepository.findById(customerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND))
		);
	}

	public CustomerProfileResponse getCustomerByQrToken(String qrToken) {
		return buildProfile(
			customerRepository.findByQrToken(qrToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND))
		);
	}

	private CustomerProfileResponse buildProfile(Customer customer) {
		Long customerId = customer.getId();
		return new CustomerProfileResponse(
			customerId,
			customer.getName(),
			customer.getPhoneNumber(),
			customer.getProfileImageUrl(),
			customer.getMembershipGrade(),
			customer.getStylePreferences(),
			countVisits(customerId),
			countStamps(customerId),
			findLastVisitedAt(customerId),
			customer.getJoinedAt()
		);
	}

	private long countVisits(Long customerId) {
		try {
			Long count = jdbcTemplate.queryForObject(
				"select count(*) from visits where customer_id = ?",
				Long.class,
				customerId
			);
			return count == null ? 0L : count;
		} catch (DataAccessException exception) {
			return 0L;
		}
	}

	private long countStamps(Long customerId) {
		try {
			Long count = jdbcTemplate.queryForObject(
				"select count(*) from visit_stamps where customer_id = ?",
				Long.class,
				customerId
			);
			return count == null ? 0L : count;
		} catch (DataAccessException exception) {
			return 0L;
		}
	}

	private LocalDateTime findLastVisitedAt(Long customerId) {
		try {
			Timestamp timestamp = jdbcTemplate.queryForObject(
				"select max(visited_at) from visits where customer_id = ?",
				Timestamp.class,
				customerId
			);
			return timestamp == null ? null : timestamp.toLocalDateTime();
		} catch (DataAccessException exception) {
			return null;
		}
	}
}
