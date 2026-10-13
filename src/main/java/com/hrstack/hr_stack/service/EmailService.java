package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/*
 * FULL REPLACEMENT of the existing EmailService.
 * Existing 3-argument sendHtmlEmail behaves exactly as before.
 * NEW: 5-argument overload that adds one attachment (Brevo "attachment" array).
 */
@Service
public class EmailService {

    private static final String BREVO_API_URL =
            "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.from-email}")
    private String fromEmail;

    @Value("${brevo.from-name:HR-Stack}")
    private String fromName;

    public EmailService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    // Unchanged behavior
    public void sendHtmlEmail(
            String to,
            String subject,
            String htmlBody) {

        send(to, subject, htmlBody, null, null);
    }

    // NEW: same email plus one attachment (e.g. QR code PNG)
    public void sendHtmlEmail(
            String to,
            String subject,
            String htmlBody,
            String attachmentName,
            byte[] attachmentBytes) {

        send(to, subject, htmlBody, attachmentName, attachmentBytes);
    }

    private void send(
            String to,
            String subject,
            String htmlBody,
            String attachmentName,
            byte[] attachmentBytes) {

        System.out.println(
                "EMAIL: Preparing Brevo email to " + to
        );

        StringBuilder json = new StringBuilder();

        json.append("{")
                .append("\"sender\":{\"name\":\"").append(escapeJson(fromName))
                .append("\",\"email\":\"").append(escapeJson(fromEmail)).append("\"},")
                .append("\"to\":[{\"email\":\"").append(escapeJson(to)).append("\"}],")
                .append("\"subject\":\"").append(escapeJson(subject)).append("\",")
                .append("\"htmlContent\":").append(toJsonString(htmlBody));

        if (attachmentName != null && attachmentBytes != null) {
            json.append(",\"attachment\":[{\"name\":\"")
                    .append(escapeJson(attachmentName))
                    .append("\",\"content\":\"")
                    .append(Base64.getEncoder().encodeToString(attachmentBytes))
                    .append("\"}]");
        }

        json.append("}");

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(BREVO_API_URL))
                            .header("accept", "application/json")
                            .header("api-key", apiKey)
                            .header("content-type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            json.toString(),
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            System.out.println(
                    "EMAIL: Sending through Brevo to " + to
            );

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int statusCode = response.statusCode();

            System.out.println(
                    "EMAIL: Brevo response status = " + statusCode
            );

            System.out.println(
                    "EMAIL: Brevo response = " + response.body()
            );

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Brevo email sending failed. HTTP "
                                + statusCode
                                + ": "
                                + response.body()
                );
            }

            System.out.println(
                    "EMAIL: Brevo email accepted successfully for " + to
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to communicate with Brevo",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Brevo email request was interrupted",
                    e
            );
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String toJsonString(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\"" + escapeJson(value) + "\"";
    }

    public String loadTemplate(String templatePath) {

        try {

            ClassPathResource resource =
                    new ClassPathResource(templatePath);

            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load email template " + templatePath,
                    e
            );
        }
    }
}
