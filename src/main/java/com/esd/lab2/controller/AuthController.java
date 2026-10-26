package com.esd.lab2.controller;

import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.service.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.esd.lab2.dto.LoginRequest;
import com.esd.lab2.dto.LoginResponse;

import com.esd.lab2.security.JwtService;
import com.esd.lab2.security.TokenDenylist;
import io.jsonwebtoken.Claims;


/**
 * S1 — FILL IN MANUALLY (no AI for this slice).
 *
 * This controller handles all /api/auth endpoints.
 * In S1 you add the signup endpoint.
 * In S2 (AI) you will add login and later logout.
 *
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenDenylist tokenDenylist;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          TokenDenylist tokenDenylist) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenDenylist = tokenDenylist;
    }

    /**
     * A1 — POST /api/auth/signup
     * Public endpoint. Returns 201 with UserResponse on success.
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    // S5: logout endpoint will be added here by AI
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // remove "Bearer "
        Claims claims = jwtService.parseToken(token);

        tokenDenylist.revoke(claims.getId(), claims.getExpiration().toInstant());

        return ResponseEntity.noContent().build();
    }
}
