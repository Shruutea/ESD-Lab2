package com.esd.lab2.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenDenylist {

    // jti -> when that token would have expired anyway
    private final ConcurrentHashMap<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, Instant expiry) {
        revoked.put(jti, expiry);
        cleanExpired();
    }

    public boolean isRevoked(String jti) {
        return revoked.containsKey(jti);
    }

    public void cleanExpired() {
        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : revoked.entrySet()) {
            if (entry.getValue().isBefore(now)) {
                revoked.remove(entry.getKey());
            }
        }
    }
}