
package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.LoginRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.security.LoginRateLimiter;
import com.hrstack.hr_stack.service.AuthService;
import com.hrstack.hr_stack.service.EmployeeService;
import com.hrstack.hr_stack.service.LoginOtpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.dto.MfaChallengeResponse;

@ExtendWith(MockitoExtension.class)
class EmployeeLoginControllerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AuthService authService;

    @Mock
    private LoginOtpService loginOtpService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private Employee employee;

    @InjectMocks
    private EmployeeController employeeController;

    @Test
    void loginWithoutTwoFactorShouldIssueLoginToken() {
        LoginRequest request =
                new LoginRequest("USER@EXAMPLE.COM", "password123");

        when(employeeService.login("USER@EXAMPLE.COM", "password123"))
                .thenReturn(employee);
        when(loginOtpService.isEnabled()).thenReturn(false);

        LoginResponse expectedResponse = mock(LoginResponse.class);
        when(authService.issueLogin(employee)).thenReturn(expectedResponse);

        ResponseEntity<?> response = employeeController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedResponse, response.getBody());

        verify(loginRateLimiter).checkAllowed("user@example.com");
        verify(loginRateLimiter).reset("user@example.com");
        verify(authService).issueLogin(employee);
        verify(loginOtpService, never()).start(any());
    }

    @Test
    void loginWithTwoFactorEnabledShouldStartOtpChallenge() {
        LoginRequest request =
                new LoginRequest("user@example.com", "password123");

        when(employeeService.login("user@example.com", "password123"))
                .thenReturn(employee);
        when(loginOtpService.isEnabled()).thenReturn(true);
        when(employee.isTwoFactorEnabled()).thenReturn(true);

        MfaChallengeResponse expectedResponse =
                mock(MfaChallengeResponse.class);

        when(loginOtpService.start(employee)).thenReturn(expectedResponse);

        ResponseEntity<?> response = employeeController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedResponse, response.getBody());

        verify(loginRateLimiter).reset("user@example.com");
        verify(loginOtpService).start(employee);
        verify(authService, never()).issueLogin(any());
    }

    @Test
    void invalidCredentialsShouldRecordFailure() {
        LoginRequest request =
                new LoginRequest("user@example.com", "wrong-password");

        when(employeeService.login("user@example.com", "wrong-password"))
                .thenThrow(new BadRequestException(
                        "Invalid email or password"));

        assertThrows(
                BadRequestException.class,
                () -> employeeController.login(request)
        );

        verify(loginRateLimiter).checkAllowed("user@example.com");
        verify(loginRateLimiter).recordFailure("user@example.com");
        verify(loginRateLimiter, never()).reset(anyString());
        verifyNoInteractions(authService);
        verify(loginOtpService, never()).start(any());
    }

    @Test
    void blockedLoginShouldNotCheckCredentials() {
        LoginRequest request =
                new LoginRequest("user@example.com", "password123");

        doThrow(new RuntimeException("Too many login attempts"))
                .when(loginRateLimiter)
                .checkAllowed("user@example.com");

        assertThrows(
                RuntimeException.class,
                () -> employeeController.login(request)
        );

        verifyNoInteractions(employeeService);
        verifyNoInteractions(authService);
        verify(loginRateLimiter, never()).recordFailure(anyString());
    }
}
