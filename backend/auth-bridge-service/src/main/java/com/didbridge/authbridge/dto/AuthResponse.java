package com.didbridge.authbridge.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
