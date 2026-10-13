package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.util.QrCodeImageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.UUID;

@Service
public class QrAttendanceEmailSender {

    private static final Logger log =
            LoggerFactory.getLogger(QrAttendanceEmailSender.class);

    private static final String TEMPLATE = "templates/email/attendance-qr.html";
    private static final String SUBJECT = "HR-Stack Attendance QR Code";

    public record Recipient(UUID id, String email, String firstName, String role) {
    }

    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;

    public QrAttendanceEmailSender(
            EmailService emailService,
            InAppNotificationService inAppNotificationService) {

        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
    }

    @Async
    public void broadcast(
            List<Recipient> recipients,
            String department,
            String validTill,
            String scanUrl,
            String qrImageUrl) {

        byte[] qrPng;
        String template;

        try {
            qrPng = QrCodeImageGenerator.png(scanUrl, 400);
            template = emailService.loadTemplate(TEMPLATE);
        } catch (Exception e) {
            log.error("QR attendance email aborted: {}", e.getMessage());
            return;
        }

        String notificationMessage =
                "Attendance QR code has been shared for " + department
                        + " department. Please check your email.";

        int sent = 0;
        int failed = 0;

        for (Recipient recipient : recipients) {

            try {
                String html = template
                        .replace("{{name}}", HtmlUtils.htmlEscape(nullToEmpty(recipient.firstName())))
                        .replace("{{department}}", HtmlUtils.htmlEscape(department))
                        .replace("{{validTill}}", HtmlUtils.htmlEscape(validTill))
                        .replace("{{scanUrl}}", HtmlUtils.htmlEscape(scanUrl))
                        .replace("{{qrImageUrl}}", HtmlUtils.htmlEscape(qrImageUrl));

                emailService.sendHtmlEmail(
                        recipient.email(),
                        SUBJECT,
                        html,
                        "hr-stack-attendance-qr.png",
                        qrPng);

                sent++;
            } catch (Exception e) {
                failed++;
                log.warn("QR attendance email failed for {}: {}",
                        recipient.email(), e.getMessage());
            }

            // Same rule as the OTP flow: in-app notification for EMPLOYEE role only
            if ("EMPLOYEE".equalsIgnoreCase(recipient.role())) {
                try {
                    inAppNotificationService.createNotification(
                            recipient.id(), "ATTENDANCE", notificationMessage);
                } catch (Exception e) {
                    log.warn("QR attendance notification failed: {}", e.getMessage());
                }
            }
        }

        log.info("QR attendance email finished: total={}, sent={}, failed={}",
                recipients.size(), sent, failed);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
