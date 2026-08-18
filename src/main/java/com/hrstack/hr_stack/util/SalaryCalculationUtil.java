package com.hrstack.hr_stack.util;

import com.hrstack.hr_stack.entity.Attendance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

public final class SalaryCalculationUtil {

    private SalaryCalculationUtil() {
        // Utility class
    }

    public static int calculateWorkingDays(YearMonth yearMonth) {

        int workingDays = 0;

        for (int day = 1;
             day <= yearMonth.lengthOfMonth();
             day++) {
            LocalDate date = yearMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY
                    && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }

    public static int calculatePresentDays(
            List<Attendance> attendances,
            YearMonth yearMonth)
    {
        int presentDays = 0;

        for (Attendance attendance : attendances) {
            if (!"PRESENT".equalsIgnoreCase(
                    attendance.getStatus())) {
                continue;
            }

            LocalDate attendanceDate =
                    Instant.ofEpochMilli(
                                    attendance.getMarkedOn()
                            )
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();

            if (YearMonth.from(attendanceDate)
                    .equals(yearMonth)) {
                presentDays++;
            }
        }
        return presentDays;
    }

    public static int calculateAbsentDays(
            int workingDays,
            int presentDays) {

        return Math.max(
                workingDays - presentDays,
                0
        );
    }

    public static BigDecimal calculateGrossSalary(
            BigDecimal basic,
            BigDecimal hra,
            BigDecimal allowances) {

        return basic
                .add(hra)
                .add(allowances)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateAbsentDeduction(
            BigDecimal grossSalary,
            int workingDays,
            int absentDays) {

        if (workingDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailySalary =
                grossSalary.divide(
                        BigDecimal.valueOf(workingDays),
                        2,
                        RoundingMode.HALF_UP
                );

        return dailySalary
                .multiply(BigDecimal.valueOf(absentDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePf(
            BigDecimal basic,
            boolean pfApplicable) {

        if (!pfApplicable) {
            return BigDecimal.ZERO;
        }

        return basic
                .multiply(BigDecimal.valueOf(0.12))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateTotalDeductions(
            BigDecimal absentDeduction,
            BigDecimal pfDeduction) {

        return absentDeduction
                .add(pfDeduction)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateNetSalary(
            BigDecimal grossSalary,
            BigDecimal totalDeductions) {

        return grossSalary
                .subtract(totalDeductions)
                .setScale(2, RoundingMode.HALF_UP);
    }
}