package com.mcm.privatecircle.global.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

	private final Long accountId;
	private final Long customerId;
	private final Long caId;
	private final Long storeId;
	private final UserRole role;

	private AuthenticatedUser(Long accountId, Long customerId, Long caId, Long storeId, UserRole role) {
		this.accountId = accountId;
		this.customerId = customerId;
		this.caId = caId;
		this.storeId = storeId;
		this.role = role;
	}

	public static AuthenticatedUser customer(Long accountId, Long customerId) {
		return new AuthenticatedUser(accountId, customerId, null, null, UserRole.CUSTOMER);
	}

	public static AuthenticatedUser ca(Long accountId, Long caId, Long storeId) {
		return new AuthenticatedUser(accountId, null, caId, storeId, UserRole.CA);
	}

	public Long getAccountId() {
		return accountId;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public Long getCaId() {
		return caId;
	}

	public Long getStoreId() {
		return storeId;
	}

	public UserRole getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return String.valueOf(accountId);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
