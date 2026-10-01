package com.floweapp.flowe_api.auth.service;

import com.floweapp.flowe_api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    public String generateAccessToken(UserDetails user) {
        return buildToken(user, properties.accessTokenExpiration(), ACCESS);
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user, properties.refreshTokenExpiration(), REFRESH);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public OffsetDateTime extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant().atOffset(ZoneOffset.UTC);
    }

    public boolean isAccessTokenValid(String token, UserDetails user) {
        return isTokenValid(token, user, ACCESS);
    }

    public boolean isRefreshTokenValid(String token, UserDetails user) {
        return isTokenValid(token, user, REFRESH);
    }

    public boolean isTokenValid(String token, UserDetails user, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            final String username = extractUsername(token);

            return username.equals(claims.getSubject())
                    && !isTokenExpired(token)
                    && expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private String buildToken(UserDetails user, long expiration, String type) {
        return Jwts.builder()
                .claim(TOKEN_TYPE_CLAIM, type)
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
