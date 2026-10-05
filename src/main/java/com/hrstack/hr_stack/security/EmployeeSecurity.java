package com.hrstack.hr_stack.security;

import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("employeeSecurity")
public class EmployeeSecurity {

    private final EmployeeRepository employeeRepository;

    public EmployeeSecurity(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // Is this UUID the logged-in user?
    public boolean isSelf(UUID id, Authentication auth) {
        return employeeRepository.findById(id)
                .map(e -> e.getEmail().equalsIgnoreCase(auth.getName()))
                .orElse(false);
    }

    // Is this empId the logged-in user's?
    public boolean isOwnEmpId(String empId, Authentication auth) {
        return employeeRepository.findByEmpId(empId)
                .map(e -> e.getEmail().equalsIgnoreCase(auth.getName()))
                .orElse(false);
    }
}