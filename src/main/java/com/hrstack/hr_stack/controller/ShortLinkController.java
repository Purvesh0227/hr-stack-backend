package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.ShortLinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Short-Link")
@RequestMapping("/s")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        String targetUrl =
                shortLinkService.resolveShortLink(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, targetUrl)
                .build();
    }

    @PostMapping("/shortlink")
    public ResponseEntity<String> createTestLink(
            @RequestParam String targetUrl) {

        String shortUrl = shortLinkService.createShortLink(
                targetUrl,
                5 * 60 * 1000L
        );

        return ResponseEntity.ok(shortUrl);
    }
}