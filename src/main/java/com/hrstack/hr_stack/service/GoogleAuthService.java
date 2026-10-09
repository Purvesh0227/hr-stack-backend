package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmployeeRepository employeeRepository;
    private final AuthService authService;

    public GoogleAuthService(GoogleTokenVerifier googleTokenVerifier,
                             EmployeeRepository employeeRepository,
                             AuthService authService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.employeeRepository = employeeRepository;
        this.authService = authService;
    }

    public LoginResponse login(String idToken) {

        GoogleTokenVerifier.GoogleUser google =
                googleTokenVerifier.verify(idToken);

        // 1. Already linked: match by Google's permanent id (sub)
        Optional<Employee> linked =
                employeeRepository.findByGoogleSub(google.sub());

        if (linked.isPresent()) {
            return authService.issueLogin(linked.get());
        }

        // 2. First Google login: match an EXISTING account by verified email
        Employee employee = employeeRepository
                .findByEmailIgnoreCase(google.email())
                .orElseThrow(() -> new BadRequestException(
                        "No HR-Stack account found for this Google email. "
                                + "Please register first."));

        // Account already linked to a different Google account
        if (employee.getGoogleSub() != null
                && !employee.getGoogleSub().equals(google.sub())) {
            throw new BadRequestException(
                    "This account is linked to a different Google account.");
        }

        // 3. Link it (only works if still unlinked)
        if (employee.getGoogleSub() == null) {
            int updated = employeeRepository.linkGoogle(
                    employee.getId(),
                    google.sub(),
                    System.currentTimeMillis());

            if (updated == 0) {
                throw new BadRequestException(
                        "Could not link Google account. Please try again.");
            }
        }

        // 4. Real login (no OTP: Google already verified this person)
        return authService.issueLogin(employee);
    }
}