package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key SECRET_KEY;
    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 hours

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            secret = "mysupersecretkeymysupersecretkey123456"; // fallback
            System.out.println("⚠️ JWT_SECRET env variable not found. Using fallback key!");
        }
        // ✅ Use raw bytes, no extra Base64 encoding
        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_TOKEN_VALIDITY);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null) {
            Object id = claims.get("userId");
            if (id instanceof Integer) return ((Integer) id).longValue();
            if (id instanceof Long) return (Long) id;
        }
        return null;
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? (String) claims.get("role") : null;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("❌ Invalid JWT token: " + e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        String username = extractUsername(token);
        return username != null && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) return true;
        Date expiration = claims.getExpiration();
        return expiration == null || expiration.before(new Date());
    }
}
