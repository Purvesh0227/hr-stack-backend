package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.exception.BadRequestException;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifier {

    public record GoogleUser(String sub, String email, String name) {}

    private static final String ISSUER_1 = "https://accounts.google.com";
    private static final String ISSUER_2 = "accounts.google.com";

    private final String googleClientId;
    private final JwtConsumer jwtConsumer;   // built once, Google keys are cached

    public GoogleTokenVerifier(
            @Value("${google.client-id:}") String googleClientId) {

        this.googleClientId = googleClientId;

        HttpsJwks jwks =
                new HttpsJwks("https://www.googleapis.com/oauth2/v3/certs");

        this.jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setRequireSubject()
                .setAllowedClockSkewInSeconds(30)
                .setExpectedIssuer(true, null)             // checked below (2 valid forms)
                .setExpectedAudience(true, googleClientId) // token must be for YOUR app
                .setVerificationKeyResolver(
                        new HttpsJwksVerificationKeyResolver(jwks))
                .setJwsAlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.PERMIT,
                        AlgorithmIdentifiers.RSA_USING_SHA256)
                .build();
    }

    public GoogleUser verify(String idToken) {

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured.");
        }

        try {
            JwtClaims claims = jwtConsumer.processToClaims(idToken);

            String issuer = claims.getIssuer();
            if (!ISSUER_1.equals(issuer) && !ISSUER_2.equals(issuer)) {
                throw new BadRequestException("Invalid Google token.");
            }

            Object verified = claims.getClaimValue("email_verified");
            if (!Boolean.TRUE.equals(verified)
                    && !"true".equals(String.valueOf(verified))) {
                throw new BadRequestException("Your Google email is not verified.");
            }

            String email = claims.getStringClaimValue("email");
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Google account has no email.");
            }

            return new GoogleUser(
                    claims.getSubject(),
                    email.trim(),
                    claims.getStringClaimValue("name"));

        } catch (InvalidJwtException | MalformedClaimException e) {
            throw new BadRequestException(
                    "Invalid or expired Google sign-in. Please try again.");
        }
    }
}