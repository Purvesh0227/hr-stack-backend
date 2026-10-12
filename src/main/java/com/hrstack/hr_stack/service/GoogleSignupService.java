package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.GoogleSignupCompleteRequest;
import com.hrstack.hr_stack.dto.GoogleSignupStartResponse;
import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

@Service
public class GoogleSignupService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final JwtService jwtService;
    private final AuthService authService;

    public GoogleSignupService(GoogleTokenVerifier googleTokenVerifier,
                               EmployeeRepository employeeRepository,
                               EmployeeService employeeService,
                               JwtService jwtService,
                               AuthService authService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    public GoogleSignupStartResponse start(String idToken) {

        GoogleTokenVerifier.GoogleUser google = googleTokenVerifier.verify(idToken);

        if (employeeRepository.existsByEmailIgnoreCase(google.email())
                || employeeRepository.findByGoogleSub(google.sub()).isPresent()) {
            throw new BadRequestException(
                    "Account already exists. Please sign in.");
        }

        String firstName = firstWord(google.givenName());
        String lastName = google.familyName() == null
                ? "" : google.familyName().replaceAll("\\s+", "");


        if (lastName.isEmpty() && google.name() != null) {
            String[] parts = google.name().trim().split("\\s+");
            if (parts.length > 1) {
                lastName = parts[parts.length - 1];
            }
        }
        if (firstName.isEmpty() && google.name() != null) {
            firstName = firstWord(google.name());
        }

        String signupToken = jwtService.generateGoogleSignupToken(
                google.sub(), google.email(), firstName, lastName);

        return new GoogleSignupStartResponse(
                signupToken, google.email(), firstName, lastName);
    }


    public LoginResponse complete(GoogleSignupCompleteRequest request) {

        JwtService.GoogleSignupClaims claims;
        try {
            claims = jwtService.parseGoogleSignupToken(request.signupToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException(
                    "Sign-up session expired. Please sign up with Google again.");
        }

        Employee employee = employeeService.registerGoogleEmployee(
                claims.email(),
                claims.sub(),
                request.firstName(),
                request.lastName(),
                request.mobile(),
                request.password());

        return authService.issueLogin(employee);
    }

    private String firstWord(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().split("\\s+")[0];
    }
}