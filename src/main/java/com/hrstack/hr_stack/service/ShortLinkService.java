package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.ShortLink;
import com.hrstack.hr_stack.exception.ShortLinkException;
import com.hrstack.hr_stack.repository.ShortLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class ShortLinkService {

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789";

    private static final int SHORT_CODE_LENGTH = 8;

    private final ShortLinkRepository repository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.short-link.base-url}")
    private String baseUrl;

    public ShortLinkService(ShortLinkRepository repository) {
        this.repository = repository;
    }


    @Transactional
    public String createShortLink(String targetUrl, long expiryMillis) {

        if (targetUrl == null || targetUrl.isBlank()) {
            throw new ShortLinkException("Target URL cannot be empty.");
        }

        if (expiryMillis <= 0) {
            throw new ShortLinkException("Expiry duration must be greater than zero.");
        }

        String shortCode = generateUniqueShortCode();

        long now = System.currentTimeMillis();

        ShortLink shortLink = new ShortLink();

        shortLink.setShortCode(shortCode);
        shortLink.setTargetUrl(targetUrl);
        shortLink.setCreatedOn(now);
        shortLink.setExpiresOn(now + expiryMillis);
        shortLink.setActive(true);

        repository.save(shortLink);

        return buildShortUrl(shortCode);
    }


    @Transactional(readOnly = true)
    public String resolveShortLink(String shortCode) {

        if (shortCode == null || shortCode.isBlank()) {
            throw new ShortLinkException("Invalid short link.");
        }

        ShortLink shortLink = repository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortLinkException("Short link not found."));

        long now = System.currentTimeMillis();

        if (!shortLink.isActive()) {
            throw new ShortLinkException("Short link is inactive.");
        }

        if (now >= shortLink.getExpiresOn()) {
            throw new ShortLinkException("Short link has expired.");
        }

        return shortLink.getTargetUrl();
    }


    private String generateUniqueShortCode() {

        for (int attempt = 0; attempt < 10; attempt++) {

            StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);

            for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
                int index = secureRandom.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }

            String shortCode = code.toString();

            if (repository.findByShortCode(shortCode).isEmpty()) {
                return shortCode;
            }
        }

        throw new ShortLinkException(
                "Unable to generate a unique short link. Please try again.");
    }

    private String buildShortUrl(String shortCode) {

        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        return normalizedBaseUrl + "/s/" + shortCode;
    }
}