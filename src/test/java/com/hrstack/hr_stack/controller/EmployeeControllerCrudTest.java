package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.security.EmployeeSecurity;
import com.hrstack.hr_stack.security.LoginRateLimiter;
import com.hrstack.hr_stack.service.AuthService;
import com.hrstack.hr_stack.service.EmployeeService;
import com.hrstack.hr_stack.service.LoginOtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EmployeeController CRUD (HTTP contract)")
class EmployeeControllerCrudTest {

    private static final UUID EMP_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private EmployeeService employeeService;
    @MockitoBean private AuthService authService;
    @MockitoBean private LoginOtpService loginOtpService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private EmployeeSecurity employeeSecurity;

    private static Employee employee() {
        Employee e = new Employee(EMP_ID, "testing", "Test",
                "user@example.com", "9999999999", "Test@1234", "EMPLOYEE");
        e.setEmpId("HRStack_2025_IT_001");
        e.setStatus("ACTIVE");
        return e;
    }

    // ============================================================
    // POST /employee/register   (multipart/form-data)
    // ============================================================
    @Nested
    @DisplayName("POST /employee/register (multipart)")
    class Register {

        @Test
        @DisplayName("missing firstName -> 400 with field error")
        void missingFirstName() throws Exception {
            mockMvc.perform(multipart("/employee/register")
                            .param("lastName", "Test")
                            .param("email", "user@example.com")
                            .param("mobile", "9999999999")
                            .param("password", "Test@1234"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.firstName").value("First name is required"));

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("bad email format -> 400")
        void badEmail() throws Exception {
            mockMvc.perform(multipart("/employee/register")
                            .param("firstName", "testing")
                            .param("lastName", "Test")
                            .param("email", "not-an-email")
                            .param("mobile", "9999999999")
                            .param("password", "Test@1234"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("weak password -> 400")
        void weakPassword() throws Exception {
            mockMvc.perform(multipart("/employee/register")
                            .param("firstName", "testing")
                            .param("lastName", "Test")
                            .param("email", "user@example.com")
                            .param("mobile", "9999999999")
                            .param("password", "short"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("bad mobile -> 400")
        void badMobile() throws Exception {
            mockMvc.perform(multipart("/employee/register")
                            .param("firstName", "testing")
                            .param("lastName", "Test")
                            .param("email", "user@example.com")
                            .param("mobile", "123")
                            .param("password", "Test@1234"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("service throws BadRequest -> controller wraps into {error: ...}")
        void duplicateFromService() throws Exception {
            when(employeeService.registerEmployee(any()))
                    .thenThrow(new BadRequestException(
                            "Email already exists. Please use another email."));

            mockMvc.perform(multipart("/employee/register")
                            .param("firstName", "testing")
                            .param("lastName", "Test")
                            .param("email", "user@example.com")
                            .param("mobile", "9999999999")
                            .param("password", "Test@1234"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(
                            "Email already exists. Please use another email."));
        }

        @Test
        @DisplayName("happy path -> 200 with employee body")
        void success() throws Exception {
            when(employeeService.registerEmployee(any())).thenReturn(employee());

            MockMultipartFile profilePhoto = new MockMultipartFile(
                    "profilePhoto", "photo.png", "image/png", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/employee/register")
                            .file(profilePhoto)
                            .param("firstName", "testing")
                            .param("lastName", "Test")
                            .param("email", "user@example.com")
                            .param("mobile", "9999999999")
                            .param("password", "Test@1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.role").value("EMPLOYEE"));
        }
    }

    // ============================================================
    // POST /employee/createAdmin   (admin-only, JSON)
    // ============================================================
    @Nested
    @DisplayName("POST /employee/createAdmin (admin only)")
    class CreateAdmin {

        @Test
        @DisplayName("no auth -> 401 (security runs before validation)")
        void unauthenticated() throws Exception {
            mockMvc.perform(post("/employee/createAdmin")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Admin","lastName":"One","email":"admin@example.com","mobile":"9999999999","password":"Test@1234"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("employee -> 403")
        void employeeForbidden() throws Exception {
            mockMvc.perform(post("/employee/createAdmin")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Admin","lastName":"One","email":"admin@example.com","mobile":"9999999999","password":"Test@1234"}
                                    """)
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("admin + weak password -> 400")
        void adminWeakPassword() throws Exception {
            mockMvc.perform(post("/employee/createAdmin")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Admin","lastName":"One","email":"admin@example.com","mobile":"9999999999","password":"short"}
                                    """)
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("admin + valid payload -> 200")
        void adminSuccess() throws Exception {
            Employee admin = employee();
            admin.setRole("ADMIN");
            when(employeeService.createAdmin(any())).thenReturn(admin);

            mockMvc.perform(post("/employee/createAdmin")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"Admin","lastName":"One","email":"admin@example.com","mobile":"9999999999","password":"Test@1234"}
                                    """)
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }

    // ============================================================
    // GET /employee/allEmployees   (admin-only)
    // ============================================================
    @Nested
    @DisplayName("GET /employee/allEmployees")
    class AllEmployees {

        @Test
        @DisplayName("employee -> 403")
        void employeeForbidden() throws Exception {
            mockMvc.perform(get("/employee/allEmployees")
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("admin -> 200 with PageResponse shape")
        void adminAllowed() throws Exception {
            PageResponse<Employee> page = new PageResponse<>(
                    List.of(employee()), 0, 10, 1, 1);
            when(employeeService.getAllEmployees(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/employee/allEmployees")
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].email").value("user@example.com"));
        }
    }

    // ============================================================
    // GET /employee/allAdmins   (admin-only)
    // ============================================================
    @Nested
    @DisplayName("GET /employee/allAdmins")
    class AllAdmins {

        @Test
        @DisplayName("employee -> 403")
        void employeeForbidden() throws Exception {
            mockMvc.perform(get("/employee/allAdmins")
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("admin -> 200")
        void adminAllowed() throws Exception {
            Employee admin = employee();
            admin.setRole("ADMIN");
            PageResponse<Employee> page = new PageResponse<>(
                    List.of(admin), 0, 10, 1, 1);
            when(employeeService.getAllAdmins(any(), any(), any(), any(Integer.class), any(Integer.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/employee/allAdmins")
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ============================================================
    // PUT /employee/{uuid}   (isSelf OR admin)
    // ============================================================
    @Nested
    @DisplayName("PUT /employee/{uuid}")
    class Update {

        @Test
        @DisplayName("employee editing another -> 403")
        void otherForbidden() throws Exception {
            when(employeeSecurity.isSelf(eq(OTHER_ID), any())).thenReturn(false);

            mockMvc.perform(put("/employee/{uuid}", OTHER_ID)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"X","lastName":"Y","mobile":"1111111111"}
                                    """)
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("employee editing self -> 200")
        void selfAllowed() throws Exception {
            when(employeeSecurity.isSelf(eq(EMP_ID), any())).thenReturn(true);
            when(employeeService.updateEmployee(eq(EMP_ID), any())).thenReturn(employee());

            mockMvc.perform(put("/employee/{uuid}", EMP_ID)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"testing","lastName":"Test","mobile":"9999999999"}
                                    """)
                            .with(user("user@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("admin editing any -> 200")
        void adminAllowed() throws Exception {
            when(employeeService.updateEmployee(eq(EMP_ID), any())).thenReturn(employee());

            mockMvc.perform(put("/employee/{uuid}", EMP_ID)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"firstName":"testing","lastName":"Test","mobile":"9999999999"}
                                    """)
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    // ============================================================
    // DELETE /employee/{uuid}   (admin only)
    // ============================================================
    @Nested
    @DisplayName("DELETE /employee/{uuid}")
    class Delete {

        @Test
        @DisplayName("employee -> 403")
        void employeeForbidden() throws Exception {
            mockMvc.perform(delete("/employee/{uuid}", EMP_ID)
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("admin -> 200 with message")
        void adminAllowed() throws Exception {
            mockMvc.perform(delete("/employee/{uuid}", EMP_ID)
                            .with(user("admin@example.com").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Employee deleted successfully"));
        }
    }

    // ============================================================
    // GET /employee/{uuid}   (isSelf OR admin)
    // ============================================================
    @Nested
    @DisplayName("GET /employee/{uuid}")
    class GetById {

        @Test
        @DisplayName("employee reading another -> 403")
        void otherForbidden() throws Exception {
            when(employeeSecurity.isSelf(eq(OTHER_ID), any())).thenReturn(false);

            mockMvc.perform(get("/employee/{uuid}", OTHER_ID)
                            .with(user("emp@example.com").roles("EMPLOYEE")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(employeeService);
        }
    }
}