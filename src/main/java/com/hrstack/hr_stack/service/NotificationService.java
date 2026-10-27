package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public NotificationService(
            EmailService emailService,
            EmployeeRepository employeeRepository) {

        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
    }


    // =========================================================
    // ATTENDANCE OTP (Now fully asynchronous)
    // =========================================================

    @Async("taskExecutor")
    public void broadcastAttendanceOtp(String otp) {

        List<Employee> employees =
                employeeRepository.findByStatusIgnoreCase("ACTIVE");

        for (Employee employee : employees) {

            try {
                sendAttendanceOtp(employee, otp);
            } catch (Exception e) {
                System.err.println(
                        "Failed to send attendance OTP to: "
                                + employee.getEmail()
                );
                e.printStackTrace();
            }
        }
    }


    @Async("taskExecutor")
    public void sendAttendanceOtp(
            Employee employee,
            String otp) {

        String htmlBody =
                emailService.loadTemplate(
                        "templates/email/attendance-otp.html"
                );

        String attendanceUrl =
                frontendUrl + "attendance";

        htmlBody = htmlBody
                .replace("{{name}}", employee.getFirstName())
                .replace("{{otp}}", otp)
                .replace("{{attendanceUrl}}", attendanceUrl);

        emailService.sendHtmlEmail(
                employee.getEmail(),
                "HR-Stack Attendance OTP",
                htmlBody
        );
    }


    // =========================================================
    // DOCUMENT VERIFICATION
    // =========================================================

    @Async("taskExecutor")
    public void sendDocumentVerificationRequest(
            Employee employee) {

        String htmlBody =
                emailService.loadTemplate(
                        "templates/email/document-verification.html"
                );

        String documentsUrl =
                frontendUrl + "/";

        htmlBody = htmlBody
                .replace("{{name}}", employee.getFirstName())
                .replace("{{documentsUrl}}", documentsUrl);

        emailService.sendHtmlEmail(
                employee.getEmail(),
                "HR-Stack - Document Verification Required",
                htmlBody
        );
    }


    // =========================================================
    // PASSWORD RESET
    // =========================================================

    @Async("taskExecutor")
    public void sendPasswordResetOtp(Employee employee, String otp) {
        String html = emailService
                .loadTemplate("templates/email/password-reset-otp.html")
                .replace("{{name}}", HtmlUtils.htmlEscape(employee.getFirstName()))
                .replace("{{otp}}", otp);

        emailService.sendHtmlEmail(
                employee.getEmail(), "HR-Stack Password Reset OTP", html);
    }


    // =========================================================
    // LOGIN OTP
    // =========================================================

    @Async("taskExecutor")
    public void sendLoginOtp(Employee employee, String otp){
        String html = emailService
                .loadTemplate("templates/email/login-otp.html")
                .replace("{{name}}", HtmlUtils.htmlEscape(employee.getFirstName()))
                .replace("{{otp}}", otp);

        emailService.sendHtmlEmail(
                employee.getEmail(), "HR-Stack Login OTP", html);
    }


    // =========================================================
    // EMAIL VERIFICATION
    // =========================================================

    @Async("taskExecutor")
    public void sendEmailVerificationOtp(String email, String otp) {
        String html = emailService
                .loadTemplate("templates/email/email-verification-otp.html")
                .replace("{{otp}}", otp);

        emailService.sendHtmlEmail(
                email, "HR-Stack Email Verification OTP", html);
    }


    @Async("taskExecutor")
    public void sendEmailVerificationLink(
            String email,
            String verificationUrl) {

        String html = emailService
                .loadTemplate("templates/email/email-verification-link.html")
                .replace("{{verificationUrl}}", HtmlUtils.htmlEscape(verificationUrl));

        emailService.sendHtmlEmail(
                email,
                "HR-Stack - Verify Your Email",
                html
        );
    }
}