package com.hrstack.hr_stack.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String loadTemplate(String templatePath) {
        try{
            ClassPathResource resource = new ClassPathResource(templatePath);

            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        }catch (IOException e){
            throw new RuntimeException("Failed to load template " + templatePath, e);
        }
    }
}
