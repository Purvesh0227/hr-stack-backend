package com.hrstack.hr_stack.dto;

import java.util.List;

public record AnalyticsResponse(
        long totalEmployees,
        long activeEmployees,
        long pendingOnboarding,
        long pendingVerification,
        AttendanceSummary attendanceToday,
        long openTickets,
        long highPriorityTickets,
        List<MonthlyCount> onboardingTrend,
        List<DailyAttendance> weeklyAttendance,
        List<RecentOnboarding> recentOnboardings,
        List<ActionItem> actionRequired
) {
    public record AttendanceSummary(long present, long absent, double percent) {
        public static AttendanceSummary of(long present, long absent) {
            long total = present + absent;
            double pct = total == 0 ? 0
                    : Math.round((present * 100.0 / total) * 10.0) / 10.0;
            return new AttendanceSummary(present, absent, pct);
        }
    }

    public record MonthlyCount(String month, long count) {}

    public record DailyAttendance(String day, long present, long absent) {}

    public record RecentOnboarding(
            String name, String role, String status, String timeAgo) {}

    public record ActionItem(
            String label, String description, String status,
            String link, String actionText) {}
}