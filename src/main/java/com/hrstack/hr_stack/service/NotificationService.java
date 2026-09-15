package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AttendanceOtpSendStatus;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;

    public NotificationService(
            EmailService emailService,
            EmployeeRepository employeeRepository) {

        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
    }


    public AttendanceOtpSendStatus broadcastAttendanceOtp(String otp) {

        List<Employee> employees =
                employeeRepository.findByStatusIgnoreCase("ACTIVE");

        int successful = 0;
        int failed = 0;

        for (Employee employee : employees) {

            System.out.println(
                    "Sending attendance OTP to: "
                            + employee.getEmail()
                            + " | Status: "
                            + employee.getStatus()
                            + " | Role: "
                            + employee.getRole()
            );

            try {
                sendAttendanceOtp(employee, otp);
                successful++;
                System.out.println("Attendance OTP sent successfully to: " + employee.getEmail());
            } catch (Exception e) {
                failed++;
                System.err.println("Failed to send attendance OTP to: " + employee.getEmail());
                e.printStackTrace();
            }
        }

        return new AttendanceOtpSendStatus(
                employees.size(),
                successful,
                failed
        );
    }

    /**
     * Builds and sends the attendance OTP email
     * to an individual employee.
     */
    private void sendAttendanceOtp(
            Employee employee,
            String otp) {

        String htmlBody =
                emailService.loadTemplate(
                        "templates/email/attendance-otp.html"
                );

        htmlBody = htmlBody
                .replace(
                        "{{name}}",
                        employee.getFirstName()
                )
                .replace(
                        "{{otp}}",
                        otp
                );

        emailService.sendHtmlEmail(
                employee.getEmail(),
                "HR-Stack Attendance OTP",
                htmlBody
        );
    }

    //for verification otp

    public void sendDocumentVerificationRequest(Employee employee){
        String htmlBody = emailService.loadTemplate("templates/email/document-verification.html");

        htmlBody = htmlBody.replace("{{name}}", employee.getFirstName());

        emailService.sendHtmlEmail(
                employee.getEmail(),
                "HR-Stack - Document Verification Required",
                htmlBody
        );
    }
}