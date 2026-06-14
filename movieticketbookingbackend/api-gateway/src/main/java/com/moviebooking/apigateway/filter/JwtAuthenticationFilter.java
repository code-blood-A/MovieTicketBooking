package com.moviebooking.apigateway.filter;

import com.moviebooking.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Filter — Runs on EVERY request through the Gateway.
 *
 * ═══════════════════════════════════════════════════════════
 * REACTIVE vs SERVLET FILTERS — KEY DIFFERENCE
 * ═══════════════════════════════════════════════════════════
 * In Spring MVC (Servlet), a filter looks like:
 *   void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
 *
 * In Spring WebFlux (Reactive), a filter looks like:
 *   Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
 *
 * WHY Mono<Void>?
 * - Mono is a reactive type that represents "0 or 1 item in the future"
 * - Mono<Void> means "a completion signal with no value" — like CompletableFuture<Void>
 * - This is non-blocking: the thread is freed while waiting for the downstream response
 * - chain.filter(exchange) returns a Mono that completes when the full request is done
 *
 * ═══════════════════════════════════════════════════════════
 * GlobalFilter vs GatewayFilter
 * ═══════════════════════════════════════════════════════════
 * GlobalFilter: Applied to ALL routes automatically. No need to configure
 *               it per route in application.yml. Perfect for auth.
 *
 * GatewayFilter: Applied to specific routes (configured per-route in yml).
 *                Used for things like AddRequestHeader for a specific service.
 *
 * We use GlobalFilter because JWT validation must happen for every request.
 *
 * ═══════════════════════════════════════════════════════════
 * Ordered interface — filter execution order
 * ═══════════════════════════════════════════════════════════
 * Lower number = higher priority = runs FIRST.
 * We use -1 so our auth filter runs before the default Gateway filters
 * (which handle routing). Auth must happen BEFORE routing.
 *
 * ═══════════════════════════════════════════════════════════
 * LLD PATTERN: Chain of Responsibility
 * ═══════════════════════════════════════════════════════════
 * Each filter in the chain decides:
 * - Handle the request (validate JWT, add headers) and pass to next: chain.filter(exchange)
 * - OR short-circuit (reject invalid token): return error response without calling chain
 *
 * This is the Chain of Responsibility pattern — each handler decides
 * whether to process and pass along, or stop the chain.
 */
@Component
@RequiredArgsConstructor  // Lombok: generates constructor with all final fields (jwtUtil)
@Slf4j                    // Lombok: generates log field (SLF4J logger)
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * PUBLIC ROUTES — These paths do NOT require a JWT token.
     *
     * WHY a whitelist approach (allow-list)?
     * Security default: DENY everything, explicitly ALLOW specific paths.
     * This is safer than a blacklist (deny-list) where you might forget to
     * block a new sensitive endpoint.
     *
     * /api/users/register  → New user signup (obviously no token yet)
     * /api/users/login     → Getting a token (no token to validate yet)
     * /api/users/refresh   → Token refresh using refresh token
     * /api/movies/**       → Movie browsing is public (no login needed to browse)
     * /api/shows/**        → Show search is public
     * /eureka/**           → Eureka dashboard (protected by its own Basic Auth)
     * /actuator/health     → Health checks (for Docker/monitoring)
     */
    /** Paths that are always public regardless of HTTP method */
    private static final List<String> ALWAYS_PUBLIC = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/refresh",
            "/eureka",
            "/actuator"
    );

    /**
     * Paths that are public ONLY for read operations (GET, HEAD).
     * POST/PUT/DELETE on these paths require a valid JWT.
     * e.g.:  GET  /api/movies      → public (browse without login)
     *        POST /api/movies      → requires JWT (admin creates movie)
     *        GET  /api/shows       → public (search shows)
     *        POST /api/shows/{id}/book → requires JWT
     */
    private static final List<String> READ_ONLY_PUBLIC = List.of(
            "/api/movies",
            "/api/theatres",
            "/api/shows"
    );

    /**
     * The core filter logic. Called for every single HTTP request.
     *
     * Flow:
     * 1. Is this path public? → skip JWT check, pass through
     * 2. Is Authorization header present and starts with "Bearer "? → if not, 401
     * 3. Extract the token (strip "Bearer " prefix)
     * 4. Is token valid (signature + expiry)? → if not, 401
     * 5. Extract claims (userId, role, email)
     * 6. Mutate the request: add X-User-Id, X-User-Role, X-User-Email headers
     * 7. Pass the MODIFIED request downstream to the target microservice
     *
     * WHY add headers? The downstream services (user-service, booking-service)
     * need to know WHO is making the request. Instead of each service parsing
     * JWT again (coupling them to JWT logic), the Gateway extracts the info
     * and passes it as plain HTTP headers. Services just read X-User-Id.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway filter — path: {}", path);

        // STEP 1: Check if route is public — bypass JWT validation
        if (isPublicPath(request)) {
            log.debug("Public path, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // STEP 2: Check Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return rejectRequest(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        // STEP 3: Extract the raw token (remove "Bearer " prefix — 7 characters)
        String token = authHeader.substring(7);

        // STEP 4: Validate token
        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            // STEP 5 & 6: Add user info as headers for downstream services
            // Services read these headers instead of parsing JWT themselves
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    claims.getSubject())                          // userId
                    .header("X-User-Role",  claims.get("role", String.class))             // ROLE_USER / ROLE_ADMIN
                    .header("X-User-Email", claims.get("email", String.class))            // user's email
                    .build();

            log.debug("JWT valid for userId: {}, role: {}", claims.getSubject(), claims.get("role"));

            // STEP 7: Continue chain with the enriched request
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            // JwtException covers: ExpiredJwtException, SignatureException,
            // MalformedJwtException, UnsupportedJwtException
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return rejectRequest(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            return rejectRequest(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }

    /**
     * Runs BEFORE the default route predicates (order = -1).
     * Auth must be checked before the request is routed anywhere.
     */
    @Override
    public int getOrder() {
        return -1;
    }

    /**
     * Checks if ANY public path prefix matches the current request path.
     *
     * We use startsWith (not equals) to handle:
     * /api/movies → matches /api/movies, /api/movies/1, /api/movies?city=Mumbai
     */
    /**
     * Determines if a request should bypass JWT validation.
     *
     * @param request the incoming request (provides both path and HTTP method)
     * @return true if JWT validation should be skipped
     */
    private boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        org.springframework.http.HttpMethod method = request.getMethod();

        // Always public — any HTTP method
        if (ALWAYS_PUBLIC.stream().anyMatch(path::startsWith)) {
            return true;
        }

        // Read-only public — only GET and HEAD are public
        if (READ_ONLY_PUBLIC.stream().anyMatch(path::startsWith)) {
            return org.springframework.http.HttpMethod.GET.equals(method)
                    || org.springframework.http.HttpMethod.HEAD.equals(method);
        }

        return false;
    }

    /**
     * Short-circuits the filter chain and returns an HTTP error response.
     *
     * WHY this pattern?
     * In WebFlux we can't just throw an exception or set a status on the response
     * and return. We must:
     * 1. Set the HTTP status on the response
     * 2. Call response.setComplete() to signal "done, send this response"
     * 3. Return the Mono — reactive streams require everything to be chained
     *
     * setComplete() returns Mono<Void> — the signal that the response is finished.
     * We return this Mono directly, which means the GatewayFilterChain is
     * NOT called (chain.filter() is never invoked) → request is rejected.
     */
    private Mono<Void> rejectRequest(ServerWebExchange exchange, HttpStatus status, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        // Add reason to response header for debugging (don't expose internals in production)
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }
}
