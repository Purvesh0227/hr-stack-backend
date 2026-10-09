package com.hrstack.hr_stack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

//reset password
    private static final String PURPOSE = "purpose";
    private static final String RESET = "PASSWORD_RESET";
    private static final long RESET_TTL_MS = 10 * 60 * 1000L;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expiration = expiration;
    }

    public String generateResetToken(String email, UUID otpId) {
        return Jwts.builder()
                .subject(email)
                .claim(PURPOSE, RESET)
                .id(otpId.toString())                 // links token to OTP row
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + RESET_TTL_MS))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseResetToken(String token) {
        Claims claims = getClaims(token);
        if (!RESET.equals(claims.get(PURPOSE, String.class))) {
            throw new JwtException("Wrong token purpose");
        }
        return claims;
    }

//    login 2fa email otp this is not login token and it says person has pass the pasword step

    private static final String MFA = "LOGIN_MFA";
    private static final long MFA_TTL_MS = 15 * 60 * 1000L;

    public String generateMfaToken(String email, UUID challengeId) {
        return Jwts.builder()
                .subject(email)
                .claim(PURPOSE, MFA)
                .id(challengeId.toString())           // links token to login_challenge row
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + MFA_TTL_MS))
                .signWith(secretKey)
                .compact();
    }

    public UUID parseMfaToken(String token) {
        Claims claims = getClaims(token);
        if (!MFA.equals(claims.get(PURPOSE, String.class))) {
            throw new JwtException("Wrong token purpose");
        }
        return UUID.fromString(claims.getId());
    }

//
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expiration)
                )
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return getClaims(token).get(PURPOSE) == null;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}