package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Otp;
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

    // Mark own attendance
    public Attendance markAttendance(String email) {

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        // Get latest OTP
        Otp otp = otpRepository
                .findTopByOrderByCreatedOnDesc()
                .orElseThrow(() ->
                        new RuntimeException("OTP not found")
                );

        // Current Unix timestamp in milliseconds
        long currentTime = System.currentTimeMillis();

        // Check OTP expiry
        if (currentTime > otp.getExpiredOn()) {
            throw new RuntimeException("OTP is expired");
        }

        // Create attendance
        Attendance attendance = new Attendance();

        attendance.setEmpId(employee.getEmpId());
        attendance.setMarkedOn(currentTime);
        attendance.setStatus("PRESENT");

        return attendanceRepository.save(attendance);
    }


    // Get logged-in employee's attendance
    public List<Attendance> getMyAttendance(String email) {

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        return attendanceRepository
                .findByEmpId(employee.getEmpId());
    }


    // ADMIN - Get all employee attendance
    public List<Attendance> getAllAttendance(String email) {

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
            throw new RuntimeException(
                    "Access denied. You are not Admin"
            );
        }

        // IMPORTANT:
        // Get attendance records, NOT employees
        return attendanceRepository.findAll();
    }
}