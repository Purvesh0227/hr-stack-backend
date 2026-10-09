package com.hrstack.hr_stack.security;

import com.hrstack.hr_stack.exception.BadRequestException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final int MAX_TRACKED_KEYS = 50_000;   // memory guard

    private static class Entry {
        int failures;
        long windowStart;
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    // call BEFORE checking the password
    public void checkAllowed(String key) {
        Entry e = entries.get(key);
        if (e == null) return;

        long now = System.currentTimeMillis();
        synchronized (e) {
            if (now - e.windowStart > WINDOW_MS) {
                entries.remove(key, e);
                return;
            }
            if (e.failures >= MAX_FAILURES) {
                long minutes = (e.windowStart + WINDOW_MS - now + 59_999) / 60_000;
                throw new BadRequestException(
                        "Too many failed login attempts. Try again in "
                                + minutes + " minute(s).");
            }
        }
    }

    // call when the password was wrong
    public void recordFailure(String key) {
        if (entries.size() > MAX_TRACKED_KEYS) return;

        long now = System.currentTimeMillis();
        Entry e = entries.computeIfAbsent(key, k -> {
            Entry n = new Entry();
            n.windowStart = now;
            return n;
        });
        synchronized (e) {
            if (now - e.windowStart > WINDOW_MS) {
                e.windowStart = now;
                e.failures = 0;
            }
            e.failures++;
        }
    }

    // call when the password was right
    public void reset(String key) {
        entries.remove(key);
    }

    @Scheduled(fixedRate = 10 * 60 * 1000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(en -> now - en.getValue().windowStart > WINDOW_MS);
    }
}