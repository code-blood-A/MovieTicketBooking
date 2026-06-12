package com.moviebooking.userservice.security;

import com.moviebooking.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtService — Issues JWT tokens at login/registration.
 *
 * ═══════════════════════════════════════════════════════════
 * RELATIONSHIP WITH API GATEWAY'S JwtUtil
 * ═══════════════════════════════════════════════════════════
 * User Service: ISSUES tokens (JwtService.generateToken)
 * API Gateway:  VALIDATES tokens (JwtUtil.extractAllClaims)
 *
 * Both use the SAME secret key (jwt.secret in each service's application.yml).
 * The secret is what makes validation possible without calling User Service.
 *
 * Think of it like a wax seal:
 * - User Service has the seal stamp (secret key) → creates the seal (signature)
 * - Gateway has a photo of the seal → can verify authenticity, can't create new ones
 * In HMAC: the same key is used for both signing and verifying (symmetric).
 *
 * ═══════════════════════════════════════════════════════════
 * WHAT GOES INTO THE JWT PAYLOAD
 * ═══════════════════════════════════════════════════════════
 * Standard claims (registered):
 *   sub  = subject = userId (who this token belongs to)
 *   iat  = issued at (Unix timestamp)
 *   exp  = expiration time (Unix timestamp)
 *
 * Custom claims (our additions):
 *   role  = "ROLE_USER" or "ROLE_ADMIN"
 *   email = user's email
 *
 * WHY add email and role to the token?
 * The Gateway reads these and passes them as headers (X-User-Role, X-User-Email)
 * to downstream services. Services can then make authorization decisions
 * (e.g., only ROLE_ADMIN can delete a movie) WITHOUT calling User Service.
 *
 * ═══════════════════════════════════════════════════════════
 * TOKEN EXPIRY STRATEGY
 * ═══════════════════════════════════════════════════════════
 * Access Token: short-lived (24h in dev, 15min in production)
 * Refresh Token: long-lived (30 days) — used to get new access tokens
 *
 * WHY short-lived access tokens?
 * If a token is stolen, it's only valid for 15min. The attacker can't do much.
 * With JWT there's NO WAY to invalidate a token before expiry (stateless).
 * Short expiry minimizes the damage window.
 *
 * We implement only access tokens in Phase 1. Refresh tokens come later.
 */
@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs; // e.g., 86400000 = 24 hours in milliseconds

    /**
     * Generates a signed JWT token for the given user.
     *
     * This token is returned to the client after login/register.
     * The client stores it (localStorage, cookie) and sends it with every
     * subsequent request in the Authorization header.
     *
     * @param user the authenticated user entity
     * @return signed JWT string (3 Base64-encoded parts separated by dots)
     */
    public String generateToken(User user) {
        // Custom claims to embed in the payload
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());    // Gateway reads this → X-User-Role header
        extraClaims.put("email", user.getEmail());          // Gateway reads this → X-User-Email header

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .claims(extraClaims)                        // Custom claims (role, email)
                .subject(String.valueOf(user.getId()))      // Standard 'sub' claim = userId
                .issuedAt(now)                              // Standard 'iat' claim
                .expiration(expiry)                         // Standard 'exp' claim
                .signWith(getSigningKey())                  // HMAC-SHA256 signature with secret key
                .compact();                                 // Build → "header.payload.signature"

        log.debug("Generated JWT for userId: {}, expires at: {}", user.getId(), expiry);
        return token;
    }

    /**
     * Validates a token and extracts all claims.
     * Used internally (and mirrors what the Gateway does during validation).
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Derives the cryptographic key from the raw secret string. */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
