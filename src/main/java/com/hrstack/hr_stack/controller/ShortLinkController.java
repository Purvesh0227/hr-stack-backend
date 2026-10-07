package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.ShortLinkService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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

    @PostMapping("/test")
    public ResponseEntity<String> createTestLink(
            @RequestParam String targetUrl) {

        String shortUrl = shortLinkService.createShortLink(
                targetUrl,
                30 * 1000L
        );

        return ResponseEntity.ok(shortUrl);
    }
}