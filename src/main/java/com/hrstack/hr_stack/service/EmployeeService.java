package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public Employee registerEmployee(
            Employee employee) {

        if (employeeRepository.existsByEmailIgnoreCase(
                employee.getEmail())) {

            throw new BadRequestException(
                    "Email already exists. Please use another email."
            );
        }

        employee.setEmpId(generateEmpId());
        employee.setRole("EMPLOYEE");

        employee.setPassword(
                encoder.encode(employee.getPassword())
        );

        return employeeRepository.save(employee);
    }

    // Create admin
    public Employee createAdmin(Employee employee) {

        if (employeeRepository.existsByEmailIgnoreCase(
                employee.getEmail())) {

            throw new BadRequestException(
                    "Email already exists. Please use another email."
            );
        }

        employee.setEmpId(generateEmpId());

        employee.setPassword(
                encoder.encode(employee.getPassword())
        );

        employee.setRole("ADMIN");

        return employeeRepository.save(employee);
    }

    // Login
    public Employee login(
            String email,
            String password) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Enter Valid Email"
                                )
                        );

        if (!encoder.matches(
                password,
                employee.getPassword())) {

            throw new BadRequestException(
                    "Enter Valid Password"
            );
        }

        return employee;
    }

    // Get all employees
    public List<Employee> getAllEmployees(
            String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        if (!"ADMIN".equalsIgnoreCase(
                employee.getRole())) {

            throw new AccessDeniedException(
                    "Access denied. You are not Admin"
            );
        }

        return employeeRepository
                .findByRoleIgnoreCase("EMPLOYEE");
    }

    // Get all admins
    public List<Employee> getAllAdmins(
            String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        if (!"ADMIN".equalsIgnoreCase(
                employee.getRole())) {

            throw new AccessDeniedException(
                    "Access denied. You are not Admin"
            );
        }

        return employeeRepository
                .findByRoleIgnoreCase("ADMIN");
    }

    // Get admin profile
    public Employee getAdminProfile(
            String email) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Admin not found"
                                )
                        );

        if (!"ADMIN".equalsIgnoreCase(
                employee.getRole())) {

            throw new AccessDeniedException(
                    "Access denied. You are not Admin"
            );
        }

        return employee;
    }

    // Find employee by email
    public Employee getEmployeeByEmail(
            String email) {

        return employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with email: "
                                        + email
                        )
                );
    }

    // Get employee by UUID
    public Employee getEmployeeById(UUID id) {

        return employeeRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found"
                        )
                );
    }

    // Delete employee by UUID
    public void deleteEmployee(UUID id) {

        if (!employeeRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Employee not found with id: " + id
            );
        }

        employeeRepository.deleteById(id);
    }

    // Generate employee ID
    public String generateEmpId() {

        int currentYear =
                Year.now().getValue();

        String prefix =
                "HRStack_" + currentYear + "_IT_";

        List<Employee> employees =
                employeeRepository.findAll();

        int maxSequence = 0;

        for (Employee employee : employees) {

            if (employee.getEmpId() != null
                    && employee.getEmpId()
                    .startsWith(prefix)) {

                String sequencePart =
                        employee.getEmpId()
                                .substring(prefix.length());

                try {

                    int sequence =
                            Integer.parseInt(sequencePart);

                    if (sequence > maxSequence) {
                        maxSequence = sequence;
                    }

                } catch (NumberFormatException ignored) {
                    // Ignore invalid employee ID values
                }
            }
        }

        int nextSequence =
                maxSequence + 1;

        return prefix +
                String.format(
                        "%03d",
                        nextSequence
                );
    }
}