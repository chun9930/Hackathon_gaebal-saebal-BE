package com.mcm.privatecircle.ai.client;

public class AiClientTimeoutException extends AiClientException {

    public AiClientTimeoutException(String message) {
        super(message);
    }

    public AiClientTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
