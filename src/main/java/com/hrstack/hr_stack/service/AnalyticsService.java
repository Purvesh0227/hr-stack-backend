package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AnalyticsResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class AnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SupportTicketRepository ticketRepository;

    public AnalyticsService(EmployeeRepository employeeRepository,
                            AttendanceRepository attendanceRepository,
                            SupportTicketRepository ticketRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.ticketRepository = ticketRepository;
    }

    public AnalyticsResponse getAnalytics() {
        // --- KPI counts (employees only, excluding admins) ---
        long totalEmployees = employeeRepository.countByRole("EMPLOYEE");
        long active = employeeRepository.countByRoleAndStatus("EMPLOYEE", "ACTIVE");
        long pendingOnboarding = employeeRepository.countByRoleAndStatus("EMPLOYEE", "PENDING");
        long pendingVerification = employeeRepository.countByRoleAndStatus("EMPLOYEE", "PENDING_VERIFICATION");

        // --- Attendance today ---
        long present = presentOn(LocalDate.now());
        long absent = Math.max(0, active - present);

        // --- Tickets ---
        long openTickets = ticketRepository.countByStatus("OPEN");
        long highPriority = ticketRepository.countByStatusAndPriority("OPEN", "HIGH");

        // --- Charts ---
        List<AnalyticsResponse.MonthlyCount> trend = buildOnboardingTrend();
        List<AnalyticsResponse.DailyAttendance> week = buildWeeklyAttendance(active);
        List<AnalyticsResponse.RecentOnboarding> recent = buildRecentOnboardings();
        List<AnalyticsResponse.ActionItem> actions =
                buildActionItems(pendingOnboarding, pendingVerification, openTickets, highPriority);

        return new AnalyticsResponse(
                totalEmployees, active, pendingOnboarding, pendingVerification,
                AnalyticsResponse.AttendanceSummary.of(present, absent),
                openTickets, highPriority,
                trend, week, recent, actions
        );
    }

    private List<AnalyticsResponse.MonthlyCount> buildOnboardingTrend() {
        List<AnalyticsResponse.MonthlyCount> trend = new ArrayList<>();
        YearMonth now = YearMonth.now();
        ZoneId zone = ZoneId.systemDefault();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            long start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
            long end = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli();
            long count = employeeRepository.countByRoleAndCreatedOnBetween("EMPLOYEE", start, end);
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            trend.add(new AnalyticsResponse.MonthlyCount(label, count));
        }
        return trend;
    }

    private List<AnalyticsResponse.DailyAttendance> buildWeeklyAttendance(long active) {
        List<AnalyticsResponse.DailyAttendance> week = new ArrayList<>();
        LocalDate day = LocalDate.now();

        // Keep going back until we find 7 working days
        while (week.size() < 7) {
            // Skip Saturday and Sunday
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                long present = presentOn(day);
                long absent = Math.max(0, active - present);
                String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                week.add(0, new AnalyticsResponse.DailyAttendance(label, present, absent)); // Add to the front
            }
            day = day.minusDays(1); // Move back one day
        }
        return week;
    }

//    private long presentOn(LocalDate day) {
//        ZoneId zone = ZoneId.systemDefault();
//        long from = day.atStartOfDay(zone).toInstant().toEpochMilli();
//        long to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;
//        return attendanceRepository.countByStatusAndMarkedOnBetween("PRESENT", from, to);
//    }

    private long presentOn(LocalDate day) {
        ZoneId zone = ZoneId.systemDefault();
        long from = day.atStartOfDay(zone).toInstant().toEpochMilli();
        long to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;
        // Use the new query that ignores PENDING employees
        return attendanceRepository.countActivePresentBetween(from, to);
    }

    private List<AnalyticsResponse.RecentOnboarding> buildRecentOnboardings() {
        List<Employee> recent = employeeRepository.findTop5ByRoleOrderByCreatedOnDesc("EMPLOYEE");
        return recent.stream()
                .limit(3)
                .map(e -> new AnalyticsResponse.RecentOnboarding(
                        safe(e.getFirstName()) + " " + safe(e.getLastName()),
                        e.getRole() != null ? e.getRole() : "Employee",
                        e.getStatus() != null ? e.getStatus() : "PENDING",
                        timeAgo(e.getCreatedOn())
                ))
                .toList();
    }

    private List<AnalyticsResponse.ActionItem> buildActionItems(
            long pendingOnboarding, long pendingVerification,
            long openTickets, long highPriority) {
        List<AnalyticsResponse.ActionItem> items = new ArrayList<>();
        if (pendingVerification > 0) {
            items.add(new AnalyticsResponse.ActionItem(
                    pendingVerification + " Awaiting Verification",
                    "Employee documents need review.",
                    "PENDING_VERIFICATION", "/employees", "Verify"));
        }
        if (pendingOnboarding > 0) {
            items.add(new AnalyticsResponse.ActionItem(
                    pendingOnboarding + " Pending Onboarding",
                    "New hires haven't completed setup.",
                    "PENDING", "/employees", "Review"));
        }
        if (openTickets > 0) {
            items.add(new AnalyticsResponse.ActionItem(
                    openTickets + " Support Tickets Open",
                    highPriority + " high priority.",
                    "OPEN", "/support-tickets", "View"));
        }
        return items;
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String timeAgo(Long millis) {
        if (millis == null) return "";
        long diff = System.currentTimeMillis() - millis;
        long mins = diff / 60_000;
        if (mins < 60) return mins + " min ago";
        long hours = mins / 60;
        if (hours < 24) return hours + " hours ago";
        return (hours / 24) + " days ago";
    }
}