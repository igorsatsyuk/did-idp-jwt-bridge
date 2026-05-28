package com.didbridge.authbridge.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidChallengeException extends RuntimeException {
    public InvalidChallengeException(String message) {
        super(message);
    }
}
