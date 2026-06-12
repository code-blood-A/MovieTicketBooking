package com.moviebooking.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT Utility — Token validation at the Gateway layer.
 *
 * ═══════════════════════════════════════════════════════════
 * THE STATELESS AUTH INSIGHT (Answer to HLD Question 5)
 * ═══════════════════════════════════════════════════════════
 * Question: "How does Gateway validate JWT without calling User Service?"
 *
 * Answer: A JWT is a SELF-CONTAINED token with 3 parts:
 *
 *   Header.Payload.Signature
 *    ↓        ↓         ↓
 *  algo     claims    HMAC(Header+Payload, secretKey)
 *
 * The Signature is a cryptographic hash of the token content,
 * created using a SECRET KEY that only YOU know.
 *
 * To VALIDATE a token, you don't need to call anyone. You just:
 *   1. Re-compute: HMAC(Header+Payload, secretKey)
 *   2. Compare with the Signature in the token
 *   3. If they match → token is genuine, nobody tampered with it
 *   4. Also check: exp (expiry) claim hasn't passed
 *
 * This is why JWT is called STATELESS — no database lookup needed.
 * The Gateway and User Service share the SAME secret key.
 *
 * ═══════════════════════════════════════════════════════════
 * WHAT'S INSIDE OUR JWT PAYLOAD (Claims)?
 * ═══════════════════════════════════════════════════════════
 * {
 *   "sub": "42",              ← userId (subject)
 *   "role": "ROLE_USER",      ← user's role
 *   "email": "user@mail.com", ← email
 *   "iat": 1718000000,        ← issued at (Unix timestamp)
 *   "exp": 1718086400         ← expires at (iat + 24h)
 * }
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: Utility Class (not a formal GoF pattern, but
 * an important structural pattern — single responsibility,
 * all JWT logic in one place, injected wherever needed)
 * ═══════════════════════════════════════════════════════════
 */
@Component
public class JwtUtil {

    /*
     * The secret key MUST be the same value as configured in User Service.
     * Both services load this from their respective application.yml files.
     *
     * WHY @Value and not a hardcoded constant?
     * - Injected from config → easy to change per environment (dev/prod)
     * - In production: inject from environment variable or secrets manager
     * - Never commit real secret keys to Git
     *
     * For HMAC-SHA256, the key should be at least 256 bits (32 characters).
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Derives a cryptographic SecretKey from the raw string secret.
     *
     * Keys.hmacShaKeyFor() converts a byte array into a javax.crypto.SecretKey
     * that JJWT can use to sign/verify tokens.
     *
     * WHY not cache this? It's derived fresh each call. Acceptable for validation.
     * In high-throughput scenarios, you'd cache it as a @Bean.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates the token and returns ALL claims if valid.
     *
     * JJWT's parser automatically:
     *   1. Verifies the HMAC signature
     *   2. Checks the 'exp' claim (throws ExpiredJwtException if expired)
     *   3. Checks the 'nbf' claim (not-before, if present)
     *
     * Throws JwtException (or subclass) for any validation failure:
     *   - ExpiredJwtException     → token expired
     *   - SignatureException       → tampered payload
     *   - MalformedJwtException   → not a valid JWT format
     *   - UnsupportedJwtException → algorithm mismatch
     *
     * The caller (JwtAuthenticationFilter) catches these and returns 401.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // Set the key for signature verification
                .build()
                .parseSignedClaims(token)     // Parse + verify in one step
                .getPayload();                // Get the Claims (payload)
    }

    /**
     * Quick validity check — returns false instead of throwing.
     * Used by the filter to decide pass-through vs reject.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Extracts the userId (stored as JWT 'subject') */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Extracts the user's role from custom claim */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /** Extracts email from custom claim */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }
}
