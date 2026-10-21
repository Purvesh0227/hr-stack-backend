
package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.LoginOtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginOtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginOtpService loginOtpService;

    @Test
    void verifyOtpWithValidRequestShouldReturnOk() throws Exception {
        mockMvc.perform(post("/employee/login/verify-otp")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mfaToken": "valid-mfa-token",
                                  "otp": "123456"
                                }
                                """))
                .andExpect(status().isOk());

        verify(loginOtpService).verify("valid-mfa-token", "123456");
    }

    @Test
    void verifyOtpWithInvalidOtpFormatShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/employee/login/verify-otp")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mfaToken": "valid-mfa-token",
                                  "otp": "12345"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginOtpService);
    }

    @Test
    void verifyOtpWithBlankMfaTokenShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/employee/login/verify-otp")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mfaToken": " ",
                                  "otp": "123456"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginOtpService);
    }

    @Test
    void resendOtpWithValidRequestShouldReturnOk() throws Exception {
        mockMvc.perform(post("/employee/login/resend-otp")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mfaToken": "valid-mfa-token"
                                }
                                """))
                .andExpect(status().isOk());

        verify(loginOtpService).resend("valid-mfa-token");
    }

    @Test
    void resendOtpWithBlankMfaTokenShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/employee/login/resend-otp")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mfaToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginOtpService);
    }
}
