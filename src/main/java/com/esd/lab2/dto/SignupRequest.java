package com.esd.lab2.dto;

import jakarta.validation.constraints.*;

/**
 * S1 — FILL IN MANUALLY (no AI for this slice).
 *
 * This is the request body for POST /api/auth/signup.
 * Add the correct jakarta.validation annotation on each field.
 *
 * Rules:
 *   - email  : must be a valid email format, required (not blank)
 *   - password: required, between 8 and 64 characters
 *   - fullName: required (not blank), max 100 characters
 */
public record SignupRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Size(max = 100)
        String fullName
) {}
