package com.examly.springapp.configuration;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for issuing and validating JWTs (HS256).
 * Works with Spring Security's Resource Server configured in SecurityConfig.
 *
 * Required properties (application.properties / YAML):
 *   app.jwt.secret=<at least 32 chars or base64-encoded 256-bit key>
 *   app.jwt.expiration=3600000
 *   app.jwt.issuer=SpringBootEmp
 */
@Component
public class JWTUtil {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;

    public JWTUtil(
            @Value("${app.jwt.secret:ThisIsADevOnlySecretChangeMeToAtLeast32Chars}") String secret,
            @Value("${app.jwt.expiration:3600000}") long expirationMs,
            @Value("${app.jwt.issuer:SpringBootEmp}") String issuer
    ) {
        this.secretKey = buildKey(secret);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    private SecretKey buildKey(String secret) {
        // Prefer Base64 if provided; otherwise use raw bytes
        try {
            byte[] key = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(key);
        } catch (IllegalArgumentException ignored) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    /** Generate a JWT for the given principal and authorities. */
    public String generateToken(String username, @Nullable Collection<? extends GrantedAuthority> authorities) {
        List<String> roles = authorities == null
                ? List.of()
                : authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    // Ensure ROLE_ prefix so hasRole("X") works without extra mapping
                    .map(a -> a.startsWith("ROLE_") ? a : "ROLE_" + a)
                    .collect(Collectors.toList());

        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles) // SecurityConfig maps this claim to GrantedAuthorities
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object claim = parse(token).getBody().get("roles");
        if (claim instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }

    public boolean isTokenValid(String token, UserDetails user) {
        try {
            return user.getUsername().equals(extractUsername(token)) && !isExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        Date exp = parse(token).getBody().getExpiration();
        return exp.before(new Date());
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }
}
