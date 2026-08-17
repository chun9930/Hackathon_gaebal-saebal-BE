package com.mcm.privatecircle.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private final String secret;
	private final Duration accessTokenValidity;

	public JwtTokenProvider(
		@Value("${app.jwt.secret:change-me-in-local}") String secret,
		@Value("${app.jwt.access-token-validity:PT2H}") Duration accessTokenValidity
	) {
		this.secret = secret;
		this.accessTokenValidity = accessTokenValidity;
	}

	public String createAccessToken(AuthenticatedUser authenticatedUser) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(String.valueOf(authenticatedUser.getAccountId()))
			.claim("accountId", authenticatedUser.getAccountId())
			.claim("customerId", authenticatedUser.getCustomerId())
			.claim("caId", authenticatedUser.getCaId())
			.claim("storeId", authenticatedUser.getStoreId())
			.claim("role", authenticatedUser.getRole().name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(accessTokenValidity)))
			.signWith(getSecretKey())
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		Long accountId = getLongClaim(claims, "accountId");
		String roleValue = claims.get("role", String.class);
		UserRole role;
		try {
			role = UserRole.valueOf(roleValue);
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN, exception);
		}

		AuthenticatedUser authenticatedUser;
		if (role == UserRole.CUSTOMER) {
			authenticatedUser = AuthenticatedUser.customer(
				accountId,
				getLongClaim(claims, "customerId")
			);
		} else if (role == UserRole.CA) {
			authenticatedUser = AuthenticatedUser.ca(
				accountId,
				getLongClaim(claims, "caId"),
				getLongClaim(claims, "storeId")
			);
		} else {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		return new UsernamePasswordAuthenticationToken(
			authenticatedUser,
			null,
			authenticatedUser.getAuthorities()
		);
	}

	public String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer == null || !bearer.startsWith("Bearer ")) {
			return null;
		}
		return bearer.substring(7);
	}

	public String getSecret() {
		return secret;
	}

	private Claims parseClaims(String token) {
		Jws<Claims> jws = Jwts.parser()
			.verifyWith(getSecretKey())
			.build()
			.parseSignedClaims(token);
		return jws.getPayload();
	}

	private Long getLongClaim(Claims claims, String key) {
		Object value = claims.get(key);
		if (value == null) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.valueOf(String.valueOf(value));
	}

	private SecretKey getSecretKey() {
		return Keys.hmacShaKeyFor(sha256(secret));
	}

	private byte[] sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
		}
	}
}
