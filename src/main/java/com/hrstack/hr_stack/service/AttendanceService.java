package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AttendanceResponse;
import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.OtpRepository;
import com.hrstack.hr_stack.util.SearchUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AttendanceService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 50;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository, OtpRepository otpRepository, PasswordEncoder passwordEncoder) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (!passwordEncoder.matches(enteredOtp, otp.getOtp())) {
            throw new BadRequestException("Invalid OTP");
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
        // attendance is already marked today ?
        LocalDate today = LocalDate.now();

        long startOfDay = today.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        long endOfDay = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        if(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                employee.getEmpId(),
                startOfDay,
                endOfDay)){
            throw new BadRequestException("Attendance already marked for today");
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

// View attendance (server-side search by ID or name + date range + pagination)
    public PageResponse<AttendanceResponse> viewAttendance(
            String email,
            String scope,
            String search,
            Long from,
            Long to,
            int page,
            int size) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Employee not found"));

        // Optional date range. Missing values mean "no limit".
        long safeFrom = from == null ? 0L : Math.max(from, 0L);
        long safeTo = to == null ? Long.MAX_VALUE : to;

        if (safeFrom > safeTo) {
            throw new BadRequestException("'from' must not be after 'to'");
        }

        // Sorting is inside the repository query, so no Sort here
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );

        // MY attendance: identity comes from the JWT, search is ignored
        if ("MY".equalsIgnoreCase(scope)) {
            return PageResponse.from(
                    attendanceRepository.findMine(
                            employee.getEmpId(), safeFrom, safeTo, pageable));
        }

        // ALL attendance - ADMIN only
        if ("ALL".equalsIgnoreCase(scope)) {

            if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
                throw new AccessDeniedException("Access denied. You are not Admin");
            }

            String term = search == null ? "" : search.trim();

            if (term.length() > MAX_SEARCH_LENGTH) {
                term = term.substring(0, MAX_SEARCH_LENGTH);
            }

            return PageResponse.from(
                    attendanceRepository.searchAll(
                            safeFrom, safeTo,
                            SearchUtils.toLikePattern(term),
                            pageable));
        }

        throw new BadRequestException("Invalid attendance scope. Use MY or ALL");
    }
}