package com.mcm.privatecircle.customer.entity;

import java.time.LocalDateTime;

import com.mcm.privatecircle.account.entity.CustomerAccount;
import com.mcm.privatecircle.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_account_id", nullable = false, unique = true)
	private CustomerAccount customerAccount;

	@Column(name = "customer_no", length = 50)
	private String customerNo;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "phone_number", nullable = false, unique = true, length = 30)
	private String phoneNumber;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "membership_grade", length = 50)
	private String membershipGrade;

	@Column(name = "qr_token", nullable = false, unique = true, length = 255)
	private String qrToken;

	@Column(name = "style_preferences", columnDefinition = "text")
	private String stylePreferences;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	protected Customer() {
	}

	public Customer(
		CustomerAccount customerAccount,
		String customerNo,
		String name,
		String phoneNumber,
		String profileImageUrl,
		String membershipGrade,
		String qrToken,
		String stylePreferences,
		LocalDateTime joinedAt
	) {
		this.customerAccount = customerAccount;
		this.customerNo = customerNo;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.profileImageUrl = profileImageUrl;
		this.membershipGrade = membershipGrade;
		this.qrToken = qrToken;
		this.stylePreferences = stylePreferences;
		this.joinedAt = joinedAt;
	}

	public Long getId() {
		return id;
	}

	public CustomerAccount getCustomerAccount() {
		return customerAccount;
	}

	public String getCustomerNo() {
		return customerNo;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public String getMembershipGrade() {
		return membershipGrade;
	}

	public String getQrToken() {
		return qrToken;
	}

	public String getStylePreferences() {
		return stylePreferences;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public void updateProfile(
		String name,
		String phoneNumber,
		String profileImageUrl,
		String membershipGrade,
		String stylePreferences
	) {
		if (name != null) {
			this.name = name;
		}
		if (phoneNumber != null) {
			this.phoneNumber = phoneNumber;
		}
		if (profileImageUrl != null) {
			this.profileImageUrl = profileImageUrl;
		}
		if (membershipGrade != null) {
			this.membershipGrade = membershipGrade;
		}
		if (stylePreferences != null) {
			this.stylePreferences = stylePreferences;
		}
	}
}
