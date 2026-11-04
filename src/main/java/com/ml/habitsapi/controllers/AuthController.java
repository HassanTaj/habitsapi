package com.ml.habitsapi.controllers;

import com.ml.habitsapi.security.TokenRevocationStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
// These routes let someone get a token and invalidate it when they sign out.
public class AuthController {

    // shortcut: the signing key and revocations are process-local; use shared key and storage for multiple instances.
    // A token stays usable for 15 minutes unless the user signs out first.
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final TokenRevocationStore tokenRevocationStore;
    private final String issuer;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            TokenRevocationStore tokenRevocationStore,
            @Value("${habitsapi.security.jwt-issuer}") String issuer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.tokenRevocationStore = tokenRevocationStore;
        this.issuer = issuer;
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        // Spring checks the supplied username and password against our configured development account.
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_LIFETIME);
        // This JWT ID (jti) is unique, so logout blocks this token without blocking the user's other tokens.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        // The header says HS256; SecurityConfig accepts tokens signed with the same algorithm.
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current JWT")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt token) {
        // Save this token ID so the decoder rejects it on later requests.
        tokenRevocationStore.revoke(token.getId(), token.getExpiresAt());
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    // These are the two values the client sends to sign in.
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    // This is the token sent back after a successful sign-in.
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
