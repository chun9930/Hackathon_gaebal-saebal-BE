package com.mcm.privatecircle.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청값 검증 실패"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "로그인 실패"),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "JWT 만료"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "JWT 오류"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한 없음"),
	FORBIDDEN_CA(HttpStatus.FORBIDDEN, "FORBIDDEN_CA", "권한 없는 CA"),
	ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."),
	CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."),
	CA_NOT_FOUND(HttpStatus.NOT_FOUND, "CA_NOT_FOUND", "직원 프로필을 찾을 수 없습니다."),
	STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "매장을 찾을 수 없습니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
	VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND", "방문을 찾을 수 없습니다."),
	VISIT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "VISIT_RECORD_NOT_FOUND", "방문 기록을 찾을 수 없습니다."),
	DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "로그인 ID가 중복됩니다."),
	DUPLICATE_PHONE_NUMBER(HttpStatus.CONFLICT, "DUPLICATE_PHONE_NUMBER", "전화번호가 중복됩니다."),
	DUPLICATE_QR_TOKEN(HttpStatus.CONFLICT, "DUPLICATE_QR_TOKEN", "QR 토큰이 중복됩니다."),
	DUPLICATE_INTEREST_PRODUCT(HttpStatus.CONFLICT, "DUPLICATE_INTEREST_PRODUCT", "관심 제품이 중복됩니다."),
	VISIT_RECORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "VISIT_RECORD_ALREADY_EXISTS", "이미 방문 기록이 존재합니다."),
	STAMP_ALREADY_ISSUED(HttpStatus.CONFLICT, "STAMP_ALREADY_ISSUED", "스탬프가 이미 발급되었습니다."),
	PRODUCT_IN_USE(HttpStatus.CONFLICT, "PRODUCT_IN_USE", "참조 중인 상품입니다."),
	INVALID_INTEREST_SOURCE(HttpStatus.BAD_REQUEST, "INVALID_INTEREST_SOURCE", "관심 제품 출처 조합이 올바르지 않습니다."),
	VISIT_CUSTOMER_MISMATCH(HttpStatus.BAD_REQUEST, "VISIT_CUSTOMER_MISMATCH", "방문과 고객이 일치하지 않습니다."),
	AI_API_TIMEOUT(HttpStatus.BAD_GATEWAY, "AI_API_TIMEOUT", "OpenAI API 시간이 초과되었습니다."),
	AI_RESPONSE_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "AI_RESPONSE_PARSE_FAILED", "OpenAI 응답 파싱에 실패했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
