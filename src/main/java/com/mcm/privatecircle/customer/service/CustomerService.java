package com.mcm.privatecircle.customer.service;

import com.mcm.privatecircle.customer.dto.CustomerActivitySummary;
import com.mcm.privatecircle.customer.dto.CustomerProfileResponse;
import com.mcm.privatecircle.customer.dto.CustomerProfileUpdateRequest;
import com.mcm.privatecircle.customer.dto.CustomerSearchResponse;
import com.mcm.privatecircle.customer.entity.Customer;
import com.mcm.privatecircle.customer.repository.CustomerRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.util.PaginationValidator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class CustomerService {

	private static final Sort SEARCH_SORT = Sort.by(
        Sort.Order.desc("joinedAt"),
        Sort.Order.desc("id")
    );

	private final CustomerRepository customerRepository;
	private final CustomerActivitySummaryReader activitySummaryReader;

	public CustomerService(
		CustomerRepository customerRepository,
		CustomerActivitySummaryReader activitySummaryReader
	) {
		this.customerRepository = customerRepository;
		this.activitySummaryReader = activitySummaryReader;
	}

	public CustomerProfileResponse getMyProfile(AuthenticatedUser authenticatedUser) {
		return buildProfile(
			customerRepository.findById(authenticatedUser.getCustomerId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND))
		);
	}

	public CustomerProfileResponse updateMyProfile(
		AuthenticatedUser authenticatedUser,
		CustomerProfileUpdateRequest request
	) {
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

    @Transactional(readOnly = true)
    public PageResponse<CustomerSearchResponse> searchCustomers(
        AuthenticatedUser authenticatedUser,
        String keyword,
        int page,
        int size
    ) {
        PaginationValidator.validate(page, size);
        if (keyword == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        PageRequest pageRequest = PageRequest.of(page, size, SEARCH_SORT);
        return PageResponse.from(
            customerRepository.searchByKeyword(
                normalizedKeyword,
                pageRequest
            ).map(this::toSearchResponse)
        );
    }

	private CustomerProfileResponse buildProfile(Customer customer) {
		Long customerId = customer.getId();
		CustomerActivitySummary activity = activitySummaryReader.read(customerId);
		return new CustomerProfileResponse(
			customerId,
			customer.getCustomerNo(),
			customer.getName(),
			customer.getPhoneNumber(),
			customer.getQrToken(),
			customer.getProfileImageUrl(),
			customer.getMembershipGrade(),
			customer.getStylePreferences(),
			activity.visitCount(),
			activity.stampCount(),
			activity.lastVisitedAt(),
			customer.getJoinedAt()
		);
	}

    private CustomerSearchResponse toSearchResponse(Customer customer) {
        return new CustomerSearchResponse(
            customer.getId(),
            customer.getCustomerNo(),
            customer.getName(),
            customer.getPhoneNumber(),
            customer.getProfileImageUrl(),
            customer.getMembershipGrade(),
            customer.getJoinedAt()
        );
    }
}
