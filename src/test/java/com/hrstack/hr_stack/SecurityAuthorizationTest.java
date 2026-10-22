package com.hrstack.hr_stack;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.SalarySlip;
import com.hrstack.hr_stack.service.AttendanceService;
import com.hrstack.hr_stack.service.EmployeeService;
import com.hrstack.hr_stack.service.OtpService;
import com.hrstack.hr_stack.service.SalaryCalculationService;
import com.hrstack.hr_stack.service.SalaryFileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private EmployeeService employeeService;
    @MockitoBean private SalaryCalculationService salaryCalculationService;
    @MockitoBean private SalaryFileStorageService salaryFileStorageService;
    @MockitoBean private AttendanceService attendanceService;
    @MockitoBean private OtpService otpService;

    private static final UUID OTHER_UUID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");



    @Test
    void unauthenticatedUserCannotAccessEmployees() throws Exception {
        mockMvc.perform(get("/employee/allEmployees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotAccessAdminOnlyEmployeesEndpoint() throws Exception {
        mockMvc.perform(get("/employee/allEmployees")
                        .with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessEmployeesEndpoint() throws Exception {
        mockMvc.perform(get("/employee/allEmployees")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }


    // salary generation admin-only


    @Test
    void salaryGenerate_requiresLogin() throws Exception {
        mockMvc.perform(post("/employee/salary/generate")
                        .param("empId", "HRStack_2025_IT_001")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void salaryGenerate_employeeIsForbidden() throws Exception {
        mockMvc.perform(post("/employee/salary/generate")
                        .param("empId", "HRStack_2025_IT_001")
                        .param("month", "1")
                        .param("year", "2025")
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void salaryGenerate_adminIsAllowed() throws Exception {
        when(salaryCalculationService.generateSalary(anyString(), anyInt(), anyInt()))
                .thenReturn(new SalarySlip());

        mockMvc.perform(post("/employee/salary/generate")
                        .param("empId", "HRStack_2025_IT_001")
                        .param("month", "1")
                        .param("year", "2025")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }


    // employee can only read own data


    @Test
    void getEmployeeById_employeeReadingOthersProfile_isForbidden() throws Exception {

        mockMvc.perform(get("/employee/{uuid}", OTHER_UUID)
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }


//attendance admin APIs


    @Test
    @DisplayName("attendance view: employee CAN call the endpoint; scope=ALL denial is enforced by the service, not the security chain")
    void attendanceView_employeeIsAuthenticatedAndAllowed() throws Exception {
        mockMvc.perform(get("/employee/attendance/view")
                        .param("scope", "ALL")
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void markAttendance_withoutLogin_isUnauthorized() throws Exception {
        mockMvc.perform(post("/employee/attendance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markAttendance_blankOtp_isBadRequest() throws Exception {
        mockMvc.perform(post("/employee/attendance")
                        .contentType("application/json")
                        .content("""
                                { "otp": " " }
                                """)
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void markAttendance_employeeCanMark_ok() throws Exception {
        when(attendanceService.markAttendance(anyString(), anyString()))
                .thenReturn(new Attendance());

        mockMvc.perform(post("/employee/attendance")
                        .contentType("application/json")
                        .content("""
                                { "otp": "123456" }
                                """)
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void createOtp_employeeIsForbidden() throws Exception {
        mockMvc.perform(post("/employee/createotp")
                        .contentType("application/json")
                        .content("""
                                { "department": "IT" }
                                """)
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}