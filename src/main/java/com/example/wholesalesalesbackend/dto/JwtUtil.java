package com.example.wholesalesalesbackend.dto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {

    private final Environment env;
    private SecretKey key;
    private long expirationMs;

    public JwtUtil(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        String jwtSecret = env.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT Secret must be at least 32 characters long");
        }

        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = Long.parseLong(env.getProperty("jwt.expiration", "3600000")); // default 1h
    }

    // === Generate Token ===
    public String generateToken(String username, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles) // store roles inside token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // === Extract Username ===
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // === Extract Roles ===
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Object rolesObj = getClaims(token).get("roles");
        if (rolesObj instanceof List) {
            return new HashSet<>((List<String>) rolesObj);
        }
        return new HashSet<>();
    }

    // === Validate Token ===
    public boolean validateToken(String token, String username) {
        return username.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
