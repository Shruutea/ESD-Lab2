package com.esd.lab2.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}