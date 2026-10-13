package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrEmailResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.QrAttendanceSession;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.QrAttendanceException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.QrAttendanceSessionRepository;
import com.hrstack.hr_stack.service.QrAttendanceEmailSender.Recipient;
import com.hrstack.hr_stack.util.QrCodeImageGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class QrAttendanceEmailService {

    private static final int MAX_TOKEN_LENGTH = 100;

    private static final DateTimeFormatter VALID_TILL_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a z", Locale.ENGLISH);

    private final QrAttendanceSessionRepository sessionRepository;
    private final EmployeeRepository employeeRepository;
    private final QrAttendanceEmailSender emailSender;
    private final String frontendUrl;
    private final String backendUrl;
    private final ZoneId zone;
    private final Set<String> testRecipients;

    public QrAttendanceEmailService(
            QrAttendanceSessionRepository sessionRepository,
            EmployeeRepository employeeRepository,
            QrAttendanceEmailSender emailSender,
            @Value("${app.frontend-url}") String frontendUrl,
            // PUBLIC address of THIS backend (already in application.properties for short links).
            // Mail apps fetch the QR image from here.
            @Value("${app.short-link.base-url}") String backendUrl,
            @Value("${app.attendance.zone:}") String zoneId,
            // SAFETY NET for local testing. Comma separated emails.
            // Blank = send to ALL active employees (production behavior).
            @Value("${app.attendance.qr.email-test-recipients:}") String testRecipients) {

        this.sessionRepository = sessionRepository;
        this.employeeRepository = employeeRepository;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
        this.backendUrl = backendUrl.endsWith("/") ? backendUrl : backendUrl + "/";
        this.zone = (zoneId == null || zoneId.isBlank())
                ? ZoneId.systemDefault()
                : ZoneId.of(zoneId.trim());
        this.testRecipients = Arrays.stream(testRecipients.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Transactional
    public QrEmailResponse sendToAllEmployees(UUID sessionId, String token) {

        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw invalidQr();
        }

        QrAttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("QR session not found"));

        boolean tokenMatches = MessageDigest.isEqual(
                sha256Hex(token.trim()).getBytes(StandardCharsets.UTF_8),
                session.getTokenHash().getBytes(StandardCharsets.UTF_8));

        if (!tokenMatches) {
            throw invalidQr();
        }

        long now = System.currentTimeMillis();

        if (QrAttendanceSession.STATUS_REVOKED.equals(session.getStatus())) {
            throw new QrAttendanceException(
                    HttpStatus.GONE, "QR_REVOKED",
                    "This QR code is no longer active.");
        }
        if (now > session.getExpiresOn()) {
            throw new QrAttendanceException(
                    HttpStatus.GONE, "QR_EXPIRED",
                    "This QR code has expired. Generate a new one.");
        }

        List<Recipient> recipients = employeeRepository
                .findByStatusIgnoreCase("ACTIVE")
                .stream()
                .filter(e -> e.getEmail() != null && !e.getEmail().isBlank())
                .filter(e -> testRecipients.isEmpty()
                        || testRecipients.contains(e.getEmail().trim().toLowerCase()))
                .map(QrAttendanceEmailService::toRecipient)
                .toList();

        if (recipients.isEmpty()) {
            throw new BadRequestException("No active employees to email.");
        }

        // Only one email broadcast per QR session (atomic)
        if (sessionRepository.markEmailed(sessionId, now) == 0) {
            throw new QrAttendanceException(
                    HttpStatus.CONFLICT, "ALREADY_EMAILED",
                    "This QR code was already emailed to employees.");
        }

        String validTill = VALID_TILL_FORMAT.format(
                Instant.ofEpochMilli(session.getExpiresOn()).atZone(zone));

        // Same shape as the QR on the admin screen
        String scanUrl = frontendUrl + "attendance/scan#t=" + token.trim();

        // Image shown inside the email body (Brevo cannot embed images, so it is hosted)
        String qrImageUrl = backendUrl + "public/attendance-qr/" + token.trim() + ".png";

        emailSender.broadcast(
                recipients, session.getDepartment(), validTill, scanUrl, qrImageUrl);

        return new QrEmailResponse(
                recipients.size(),
                "Emailing QR code to " + recipients.size() + " employee(s).");
    }

    @Transactional(readOnly = true)
    public byte[] renderQrImage(String token) {

        QrAttendanceSession session = sessionRepository
                .findByTokenHash(sha256Hex(token))
                .orElseThrow(() -> new QrAttendanceException(
                        HttpStatus.NOT_FOUND, "QR_INVALID", "Not found"));

        if (QrAttendanceSession.STATUS_REVOKED.equals(session.getStatus())
                || System.currentTimeMillis() > session.getExpiresOn()) {
            throw new QrAttendanceException(
                    HttpStatus.GONE, "QR_EXPIRED", "QR code expired");
        }

        return QrCodeImageGenerator.png(
                frontendUrl + "attendance/scan#t=" + token, 400);
    }

    private static Recipient toRecipient(Employee e) {
        return new Recipient(e.getId(), e.getEmail().trim(), e.getFirstName(), e.getRole());
    }

    private static QrAttendanceException invalidQr() {
        return new QrAttendanceException(
                HttpStatus.BAD_REQUEST, "QR_INVALID", "Invalid QR code.");
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
