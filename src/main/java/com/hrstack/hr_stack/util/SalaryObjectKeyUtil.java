package com.hrstack.hr_stack.util;

public final class SalaryObjectKeyUtil {

    private static final String SALARY_SLIPS_PREFIX = "salary-slips";

    private SalaryObjectKeyUtil() {
    }

    public static String buildSalarySlipObjectKey(
            String empId,
            int month,
            int year) {

        return SALARY_SLIPS_PREFIX
                + "/"
                + year
                + "/"
                + month
                + "/"
                + empId
                + ".pdf";
    }
}