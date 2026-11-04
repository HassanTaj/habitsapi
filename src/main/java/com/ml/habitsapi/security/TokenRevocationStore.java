package com.ml.habitsapi.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
// This small list remembers which JWTs have already signed out.
public class TokenRevocationStore {

    // Each key is a token ID; its value is when that token expires.
    private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        // Remove old entries first so expired tokens do not stay in memory forever.
        discardExpired();
        revokedTokens.put(tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        // A token is blocked only until it expires naturally.
        Instant expiresAt = revokedTokens.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(Instant.now())) {
            return true;
        }
        revokedTokens.remove(tokenId, expiresAt);
        return false;
    }

    private void discardExpired() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
