
package com.hrstack.hr_stack;

import com.hrstack.hr_stack.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

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
}
