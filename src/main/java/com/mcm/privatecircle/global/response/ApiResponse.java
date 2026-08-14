package com.mcm.privatecircle.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private final boolean success;
	private final T data;
	private final String message;
	private final ErrorDetail error;

	private ApiResponse(boolean success, T data, String message, ErrorDetail error) {
		this.success = success;
		this.data = data;
		this.message = message;
		this.error = error;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, "요청이 성공했습니다.", null);
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, data, message, null);
	}

	public static <T> ApiResponse<T> fail(ErrorDetail error) {
		return new ApiResponse<>(false, null, null, error);
	}

	public boolean isSuccess() {
		return success;
	}

	public T getData() {
		return data;
	}

	public String getMessage() {
		return message;
	}

	public ErrorDetail getError() {
		return error;
	}
}
