package com.didbridge.authbridge.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TokenRateLimitExceededException extends RuntimeException {
    public TokenRateLimitExceededException(String message) {
        super(message);
    }
}
