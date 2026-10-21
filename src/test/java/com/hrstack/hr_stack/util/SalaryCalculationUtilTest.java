package com.hrstack.hr_stack.util;

import com.hrstack.hr_stack.entity.Attendance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SalaryCalculationUtil")
class SalaryCalculationUtilTest {

    // ---------- helpers ----------

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    /** Compare money by value, so 0 and 0.00 are treated as equal. */
    private static void assertMoney(String expected, BigDecimal actual) {
        assertTrue(bd(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    private static Attendance attendance(String status, String isoUtcTime) {
        Attendance a = new Attendance();
        a.setStatus(status);
        a.setMarkedOn(Instant.parse(isoUtcTime).toEpochMilli());
        return a;
    }

    // ---------- working days ----------

    @Nested
    @DisplayName("calculateWorkingDays")
    class WorkingDays {

        @ParameterizedTest(name = "{0}-{1} has {2} working days")
        @CsvSource({
                "2025, 1, 23",
                "2025, 3, 21",
                "2026, 2, 20",   // 28 days
                "2024, 2, 21"    // leap year, 29 days
        })
        void returnsWeekdaysOnly(int year, int month, int expected) {
            assertEquals(expected,
                    SalaryCalculationUtil.calculateWorkingDays(YearMonth.of(year, month)));
        }
    }

    // ---------- present days ----------

    @Nested
    @DisplayName("calculatePresentDays")
    class PresentDays {

        @Test
        @DisplayName("counts PRESENT (any case) in requested month, ignores ABSENT and other months")
        void countsOnlyPresentInRequestedMonth() {
            List<Attendance> list = List.of(
                    attendance("PRESENT", "2025-03-03T09:00:00Z"),  // counts
                    attendance("present", "2025-03-04T09:00:00Z"),  // counts (lowercase)
                    attendance("ABSENT",  "2025-03-05T09:00:00Z"),  // ignored: not present
                    attendance("PRESENT", "2025-02-28T23:59:59Z"),  // ignored: February
                    attendance("PRESENT", "2025-03-31T23:59:59Z"),  // counts: last second of March
                    attendance("PRESENT", "2025-04-01T00:00:00Z")   // ignored: April
            );

            assertEquals(3,
                    SalaryCalculationUtil.calculatePresentDays(list, YearMonth.of(2025, 3)));
        }

        @Test
        @DisplayName("returns 0 for empty list")
        void emptyList() {
            assertEquals(0,
                    SalaryCalculationUtil.calculatePresentDays(List.of(), YearMonth.of(2025, 3)));
        }
    }

    // ---------- absent days ----------

    @Nested
    @DisplayName("calculateAbsentDays")
    class AbsentDays {

        @ParameterizedTest(name = "working={0}, present={1} -> absent={2}")
        @CsvSource({
                "22, 20, 2",
                "22, 22, 0",
                "20, 25, 0"   // never negative
        })
        void workingMinusPresent_neverNegative(int working, int present, int expected) {
            assertEquals(expected, SalaryCalculationUtil.calculateAbsentDays(working, present));
        }
    }

    // ---------- gross ----------

    @Nested
    @DisplayName("calculateGrossSalary")
    class Gross {

        @Test
        @DisplayName("adds basic + hra + allowances")
        void sumsComponents() {
            assertMoney("45000.00",
                    SalaryCalculationUtil.calculateGrossSalary(bd("30000"), bd("12000"), bd("3000")));
        }

        @Test
        @DisplayName("rounds to 2 decimals using HALF_UP")
        void roundsHalfUp() {
            assertMoney("10000.51",
                    SalaryCalculationUtil.calculateGrossSalary(bd("10000.505"), bd("0"), bd("0")));
        }
    }

    // ---------- absent deduction ----------

    @Nested
    @DisplayName("calculateAbsentDeduction")
    class AbsentDeduction {

        @ParameterizedTest(name = "gross={0}, working={1}, absent={2} -> {3}")
        @CsvSource({
                "45000.00, 22, 2, 4090.90",
                "10000.00, 22, 1, 454.55",   // daily rate rounded first
                "45000.00, 22, 0, 0.00",     // no absence
                "45000.00, 0,  3, 0.00"      // zero working days, no divide by zero
        })
        void calculatesDailyRateTimesAbsentDays(
                String gross, int working, int absent, String expected) {
            assertMoney(expected,
                    SalaryCalculationUtil.calculateAbsentDeduction(bd(gross), working, absent));
        }
        @Test
        @DisplayName("is capped at gross when rounding would exceed it (full-month absence)")
        void cappedAtGross() {
            // 2142.86 x 21 = 45000.06 without the cap
            assertMoney("45000.00",
                    SalaryCalculationUtil.calculateAbsentDeduction(bd("45000.00"), 21, 21));
        }

    }

    // ---------- PF ----------

    @Nested
    @DisplayName("calculatePf")
    class Pf {

        @Test
        @DisplayName("is 12% of basic when applicable")
        void applicable() {
            assertMoney("3600.00", SalaryCalculationUtil.calculatePf(bd("30000"), true));
        }

        @Test
        @DisplayName("is zero when not applicable")
        void notApplicable() {
            assertMoney("0", SalaryCalculationUtil.calculatePf(bd("30000"), false));
        }
    }

    // ---------- totals and net ----------

    @Nested
    @DisplayName("totals and net salary")
    class TotalsAndNet {

        @Test
        @DisplayName("total deductions = absent deduction + PF")
        void totalDeductions() {
            assertMoney("7690.90",
                    SalaryCalculationUtil.calculateTotalDeductions(bd("4090.90"), bd("3600.00")));
        }

        @Test
        @DisplayName("net = gross - total deductions")
        void netSalary() {
            assertMoney("37309.10",
                    SalaryCalculationUtil.calculateNetSalary(bd("45000.00"), bd("7690.90")));
        }

        @Test
        @DisplayName("full flow: 45000 gross, 2 absent of 22 days, PF on -> net 37309.10")
        void fullFlow() {
            BigDecimal gross = SalaryCalculationUtil.calculateGrossSalary(bd("30000"), bd("12000"), bd("3000"));
            int absentDays = SalaryCalculationUtil.calculateAbsentDays(22, 20);
            BigDecimal absentDeduction = SalaryCalculationUtil.calculateAbsentDeduction(gross, 22, absentDays);
            BigDecimal pf = SalaryCalculationUtil.calculatePf(bd("30000"), true);
            BigDecimal total = SalaryCalculationUtil.calculateTotalDeductions(absentDeduction, pf);
            BigDecimal net = SalaryCalculationUtil.calculateNetSalary(gross, total);

            assertMoney("4090.90", absentDeduction);
            assertMoney("7690.90", total);
            assertMoney("37309.10", net);
        }
    }
}