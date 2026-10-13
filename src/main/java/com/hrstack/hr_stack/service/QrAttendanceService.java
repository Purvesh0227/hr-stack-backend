package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrMarkResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionStatusResponse;
import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.QrAttendanceSession;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.QrAttendanceException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.QrAttendanceSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class QrAttendanceService {


    private static final long SESSION_TTL_MS = 5 * 60 * 1000L;

    private static final int TOKEN_BYTES = 32;
    private static final int MAX_TOKEN_LENGTH = 100;
    private static final int MAX_DEPARTMENT_LENGTH = 100;

    private final SecureRandom secureRandom = new SecureRandom();

    private final QrAttendanceSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ZoneId zone;

    public QrAttendanceService(
            QrAttendanceSessionRepository sessionRepository,
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            @Value("${app.attendance.zone:}") String zoneId) {

        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.zone = (zoneId == null || zoneId.isBlank())
                ? ZoneId.systemDefault()
                : ZoneId.of(zoneId.trim());
    }

//admin creates session
    @Transactional
    public QrSessionResponse createSession(String adminEmail, String department) {
        Employee admin = findEmployee(adminEmail);

        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new AccessDeniedException("Access denied. You are not Admin");
        }

        String dept = department == null ? "" : department.trim();

        if (dept.isEmpty()) {
            throw new BadRequestException("Department is required.");
        }
        if (dept.length() > MAX_DEPARTMENT_LENGTH) {
            throw new BadRequestException("Department is too long.");
        }

        // New QR replaces old one for this department
        sessionRepository.revokeActiveByDepartment(dept);

        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        long now = System.currentTimeMillis();

        QrAttendanceSession session = new QrAttendanceSession();
        session.setTokenHash(sha256Hex(token));
        session.setCreatedBy(admin.getId());
        session.setDepartment(dept);
        session.setCreatedOn(now);
        session.setExpiresOn(now + SESSION_TTL_MS);
        session.setStatus(QrAttendanceSession.STATUS_ACTIVE);

        QrAttendanceSession saved = sessionRepository.save(session);

        return new QrSessionResponse(
                saved.getUuid(),
                token,
                saved.getDepartment(),
                saved.getCreatedOn(),
                saved.getExpiresOn(),
                now);
    }


    @Transactional(readOnly = true)
    public QrSessionStatusResponse getSessionStatus(UUID sessionId) {

        QrAttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("QR session not found"));

        long now = System.currentTimeMillis();

        String status;
        if (QrAttendanceSession.STATUS_REVOKED.equals(session.getStatus())) {
            status = "REVOKED";
        } else if (now > session.getExpiresOn()) {
            status = "EXPIRED";
        } else {
            status = "ACTIVE";
        }

        return new QrSessionStatusResponse(
                session.getUuid(),
                session.getDepartment(),
                status,
                session.getExpiresOn(),
                now,
                attendanceRepository.countBySessionId(session.getUuid()));
    }


    @Transactional
    public void revokeSession(UUID sessionId) {

        QrAttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("QR session not found"));

        session.setStatus(QrAttendanceSession.STATUS_REVOKED);
        sessionRepository.save(session);
    }



    @Transactional
    public QrMarkResponse markAttendance(String email, String token) {

        // 1. Token shape
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw invalidQr();
        }

        // 2. Session lookup (hash lookup, token is 256-bit random)
        QrAttendanceSession session = sessionRepository
                .findByTokenHash(sha256Hex(token.trim()))
                .orElseThrow(QrAttendanceService::invalidQr);

        long now = System.currentTimeMillis();

        // 3. Revoked / expired (server clock only)
        if (QrAttendanceSession.STATUS_REVOKED.equals(session.getStatus())) {
            throw new QrAttendanceException(
                    HttpStatus.GONE,
                    "QR_REVOKED",
                    "This QR code is no longer active. Ask your admin for a new one.");
        }
        if (now > session.getExpiresOn()) {
            throw new QrAttendanceException(
                    HttpStatus.GONE,
                    "QR_EXPIRED",
                    "This QR code has expired. Ask your admin for a new one.");
        }

        // 4. Employee comes from JWT, never from request body
        Employee employee = findEmployee(email);

        boolean activeAccount = "ACTIVE".equalsIgnoreCase(employee.getStatus());
        boolean allowedRole = "ADMIN".equalsIgnoreCase(employee.getRole())
                || "EMPLOYEE".equalsIgnoreCase(employee.getRole());

        if (!activeAccount || !allowedRole) {
            throw new QrAttendanceException(
                    HttpStatus.FORBIDDEN,
                    "NOT_ELIGIBLE",
                    "Your account is not eligible to mark attendance.");
        }

        // 5. Already marked today? (same window logic as OTP flow)
        LocalDate today = LocalDate.now(zone);
        long startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli();
        long endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();

        if (attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                employee.getEmpId(), startOfDay, endOfDay)) {
            throw alreadyMarked();
        }

        // 6. Save. Unique index (emp_id, attendance_date) stops double-tap race.
        Attendance attendance = new Attendance();
        attendance.setEmpId(employee.getEmpId());
        attendance.setMarkedOn(now);
        attendance.setStatus("PRESENT");
        attendance.setMethod("QR");
        attendance.setSessionId(session.getUuid());
        attendance.setAttendanceDate(today);

        try {
            attendanceRepository.saveAndFlush(attendance);
        } catch (DataIntegrityViolationException ex) {
            throw alreadyMarked();
        }

        return new QrMarkResponse(now, "PRESENT", "QR");
    }



    private Employee findEmployee(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));
    }


    private static QrAttendanceException invalidQr() {
        return new QrAttendanceException(
                HttpStatus.BAD_REQUEST,
                "QR_INVALID",
                "Invalid QR code. Please scan the QR code shown by your admin.");
    }

    private static QrAttendanceException alreadyMarked() {
        return new QrAttendanceException(
                HttpStatus.CONFLICT,
                "ALREADY_MARKED",
                "Attendance already marked for today.");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}