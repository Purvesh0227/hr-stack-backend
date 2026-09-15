package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeDocumentRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.hrstack.hr_stack.service.NotificationService;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import com.hrstack.hr_stack.entity.EmployeeDocument;
import com.hrstack.hr_stack.repository.EmployeeDocumentRepository;

import org.springframework.beans.factory.annotation.Value;
import com.hrstack.hr_stack.enums.EmployeeStatus;
@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    @Value("${minio.permanent-bucket}")
    private String permanentBucket;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    @Autowired
    private NotificationService notificationService;


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

    // Update employee details
    public Employee updateEmployee(
            UUID id,
            Employee updatedEmployee) {

        Employee existingEmployee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        existingEmployee.setFirstName(
                updatedEmployee.getFirstName()
        );

        existingEmployee.setLastName(
                updatedEmployee.getLastName()
        );

        existingEmployee.setMobile(
                updatedEmployee.getMobile()
        );

        return employeeRepository.save(existingEmployee);
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

    //request employee docs

    public Employee requestDocuments(UUID id) {
        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found with id: " + id));

        employee.setStatus(EmployeeStatus.PENDING_VERIFICATION.name());

        Employee savedEmployee = employeeRepository.save(employee);

        notificationService.sendDocumentVerificationRequest(savedEmployee);

        return savedEmployee;
    }

    // Activate employee after document verification
    public Employee activateEmployee(UUID id) {

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        if (!EmployeeStatus.PENDING_VERIFICATION.name()
                .equalsIgnoreCase(employee.getStatus())) {

            throw new BadRequestException(
                    "Employee is not pending document verification"
            );
        }

        EmployeeDocument documents =
                employeeDocumentRepository
                        .findByEmployeeId(id)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Employee documents not submitted"
                                )
                        );

        if (documents.getIdProofType() == null
                || documents.getIdProofNumber() == null
                || documents.getIdProofObjectKey() == null) {

            throw new BadRequestException(
                    "ID proof is incomplete"
            );
        }

        if (documents.getAddressProofType() == null
                || documents.getAddressProofNumber() == null
                || documents.getAddressProofObjectKey() == null) {

            throw new BadRequestException(
                    "Address proof is incomplete"
            );
        }

        // Verify ID proof exists in TEMP bucket
        if (!minioStorageService.exists(
                tempBucket,
                documents.getIdProofObjectKey())) {

            throw new BadRequestException(
                    "ID proof file not found in temporary storage"
            );
        }

// Verify address proof exists in TEMP bucket
        if (!minioStorageService.exists(
                tempBucket,
                documents.getAddressProofObjectKey())) {

            throw new BadRequestException(
                    "Address proof file not found in temporary storage"
            );
        }

// Move ID proof to permanent bucket
        minioStorageService.move(
                tempBucket,
                documents.getIdProofObjectKey(),
                permanentBucket,
                documents.getIdProofObjectKey()
        );

// Move address proof to permanent bucket
        minioStorageService.move(
                tempBucket,
                documents.getAddressProofObjectKey(),
                permanentBucket,
                documents.getAddressProofObjectKey()
        );

// Activate employee
        employee.setStatus(EmployeeStatus.ACTIVE.name());

        return employeeRepository.save(employee);
    }
}