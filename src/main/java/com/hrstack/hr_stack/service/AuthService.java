package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Single place that creates a real logged-in session.
    public LoginResponse issueLogin(Employee employee) {
        String token = jwtService.generateToken(
                employee.getEmail(),
                employee.getRole()
        );
        return new LoginResponse(token, employee);
    }
}