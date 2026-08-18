package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final OtpRepository otpRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            OtpRepository otpRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.otpRepository = otpRepository;
    }

    // Mark attendance
    public Attendance markAttendance(
            String email,
            String enteredOtp) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        // Get latest OTP
        Otp otp =
                otpRepository
                        .findTopByOrderByCreatedOnDesc()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "OTP not found"
                                )
                        );

        // Check OTP value
        if (!otp.getOtp().equals(enteredOtp)) {

            throw new BadRequestException(
                    "Invalid OTP"
            );
        }

        // Current Unix timestamp
        long currentTime =
                System.currentTimeMillis();

        // Check expiry
        if (currentTime > otp.getExpiredOn()) {

            throw new BadRequestException(
                    "OTP is expired"
            );
        }

        // Create attendance
        Attendance attendance = new Attendance();

        attendance.setEmpId(
                employee.getEmpId()
        );

        attendance.setMarkedOn(currentTime);
        attendance.setStatus("PRESENT");

        return attendanceRepository.save(
                attendance
        );
    }

    // View attendance
    public List<Attendance> viewAttendance(
            String email,
            String scope) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        // MY attendance
        if ("MY".equalsIgnoreCase(scope)) {

            return attendanceRepository
                    .findByEmpId(employee.getEmpId());
        }

        // ALL attendance - ADMIN only
        if ("ALL".equalsIgnoreCase(scope)) {

            if (!"ADMIN".equalsIgnoreCase(
                    employee.getRole())) {

                throw new AccessDeniedException(
                        "Access denied. You are not Admin"
                );
            }

            return attendanceRepository.findAll();
        }

        throw new BadRequestException(
                "Invalid attendance scope. Use MY or ALL"
        );
    }
}