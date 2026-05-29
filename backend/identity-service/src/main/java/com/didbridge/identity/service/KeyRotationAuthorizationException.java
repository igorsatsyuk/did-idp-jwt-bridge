package com.didbridge.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class KeyRotationAuthorizationException extends RuntimeException {
    public KeyRotationAuthorizationException() {
        super("Invalid key rotation token");
    }
}
