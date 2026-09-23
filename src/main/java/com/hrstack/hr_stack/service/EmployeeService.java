package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.EmployeeProfileResponse;
import com.hrstack.hr_stack.dto.RegisterEmployeeRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.EmployeeDocument;
import com.hrstack.hr_stack.enums.EmployeeStatus;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeDocumentRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.io.FilenameUtils.getExtension;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private MinioStorageService minioStorageService;

    @Value("${s3.temp-bucket}")
    private String tempBucket;

    @Value("${s3.permanent-bucket}")
    private String permanentBucket;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    @Autowired
    private NotificationService notificationService;


    // =========================================================
    // REGISTER EMPLOYEE
    // =========================================================

    public Employee registerEmployee(
            RegisterEmployeeRequest request) {

        if (employeeRepository.existsByEmailIgnoreCase(
                request.getEmail())) {

            throw new BadRequestException(
                    "Email already exists. Please use another email."
            );
        }

        Employee employee = new Employee();

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setMobile(request.getMobile());

        employee.setEmpId(generateEmpId());
        employee.setRole("EMPLOYEE");

        employee.setPassword(
                encoder.encode(request.getPassword())
        );

        // Profile photo
        if (request.getProfilePhoto() != null
                && !request.getProfilePhoto().isEmpty()) {

            MultipartFile profilePhoto =
                    request.getProfilePhoto();

            validateProfilePhoto(profilePhoto);

            try {

                String objectKey =
                        "employee-profile/"
                                + employee.getEmpId()
                                + "/profile-picture"
                                + getExtension(
                                profilePhoto.getOriginalFilename()
                        );

                minioStorageService.upload(
                        permanentBucket,
                        objectKey,
                        profilePhoto.getBytes(),
                        profilePhoto.getContentType()
                );

                employee.setProfilePhotoObjectKey(objectKey);

            } catch (Exception e) {

                throw new RuntimeException(
                        "Failed to upload profile photo",
                        e
                );
            }
        }

        long currentTime = System.currentTimeMillis();
        employee.setCreatedOn(currentTime);
        employee.setUpdatedOn(currentTime);

        return employeeRepository.save(employee);
    }


    // =========================================================
    // PROFILE PHOTO VALIDATION
    // =========================================================

    private void validateProfilePhoto(MultipartFile file) {

        long maxSize = 2 * 1024 * 1024;

        if (file.getSize() > maxSize) {

            throw new BadRequestException(
                    "Profile photo must not exceed 2 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || (!contentType.equals("image/jpeg")
                && !contentType.equals("image/png"))) {

            throw new BadRequestException(
                    "Only JPG, JPEG and PNG profile photos are allowed."
            );
        }
    }


    // =========================================================
    // GET FILE EXTENSION
    // =========================================================

    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {

            return ".jpg";
        }

        return fileName
                .substring(fileName.lastIndexOf("."))
                .toLowerCase();
    }


    // =========================================================
    // CREATE ADMIN
    // =========================================================

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


        long currentTime = System.currentTimeMillis();
        employee.setCreatedOn(currentTime);
        employee.setUpdatedOn(currentTime);

        return employeeRepository.save(employee);
    }


    // =========================================================
    // LOGIN
    // =========================================================

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


    // =========================================================
    // GET ALL EMPLOYEES
    // =========================================================

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


    // =========================================================
    // GET ALL ADMINS
    // =========================================================

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


    // =========================================================
    // GET ADMIN PROFILE
    // =========================================================

    public EmployeeProfileResponse getAdminProfile(
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

        String profilePhotoUrl = null;

        if (employee.getProfilePhotoObjectKey() != null
                && !employee.getProfilePhotoObjectKey().isBlank()) {

            profilePhotoUrl =
                    minioStorageService.getSignedUrl(
                            permanentBucket,
                            employee.getProfilePhotoObjectKey()
                    );
        }

        return new EmployeeProfileResponse(
                employee.getId().toString(),
                employee.getEmpId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getMobile(),
                employee.getRole(),
                employee.getStatus(),
                profilePhotoUrl,
                employee.getCreatedOn(),
                employee.getUpdatedOn(),
                null
        );
    }


    // =========================================================
    // FIND EMPLOYEE BY EMAIL
    // =========================================================

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


    // =========================================================
    // GET EMPLOYEE PROFILE BY UUID
    // =========================================================

    public EmployeeProfileResponse getEmployeeById(
            UUID id) {

        Employee employee =
                employeeRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found with id: "
                                                + id
                                )
                        );

        String profilePhotoUrl = null;

        if (employee.getProfilePhotoObjectKey() != null
                && !employee.getProfilePhotoObjectKey().isBlank()) {

            profilePhotoUrl =
                    minioStorageService.getSignedUrl(
                            permanentBucket,
                            employee.getProfilePhotoObjectKey()
                    );
        }

        // Fetch uploaded employee documents
        EmployeeDocument documents =
                employeeDocumentRepository
                        .findByEmployeeId(id)
                        .orElse(null);

        return new EmployeeProfileResponse(
                employee.getId().toString(),
                employee.getEmpId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getMobile(),
                employee.getRole(),
                employee.getStatus(),
                profilePhotoUrl,
                employee.getCreatedOn(),
                employee.getUpdatedOn(),
                documents
        );
    }

    // =========================================================
    // UPDATE EMPLOYEE DETAILS
    // =========================================================

    public Employee updateEmployee(
            UUID id,
            Employee updatedEmployee) {

        Employee existingEmployee =
                employeeRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        existingEmployee.setFirstName(updatedEmployee.getFirstName());

        existingEmployee.setLastName(updatedEmployee.getLastName());

        existingEmployee.setMobile(updatedEmployee.getMobile());

        existingEmployee.setUpdatedOn(System.currentTimeMillis());

        return employeeRepository.save(existingEmployee);
    }

    // =========================================================
    // DELETE EMPLOYEE
    // =========================================================

    public void deleteEmployee(UUID id) {

        if (!employeeRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Employee not found"
            );
        }

        employeeRepository.deleteById(id);
    }


    // =========================================================
    // GENERATE EMPLOYEE ID
    // =========================================================

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


    // =========================================================
    // REQUEST EMPLOYEE DOCUMENTS
    // =========================================================

    public Employee requestDocuments(UUID id) {

        Employee employee =
                employeeRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Employee not found with id: " + id)
                        );

        employee.setStatus(EmployeeStatus.PENDING_VERIFICATION.name());

        employee.setUpdatedOn(System.currentTimeMillis());

        Employee savedEmployee =
                employeeRepository.save(employee);

        notificationService
                .sendDocumentVerificationRequest(
                        savedEmployee
                );

        return savedEmployee;
    }


    // =========================================================
    // ACTIVATE EMPLOYEE AFTER DOCUMENT VERIFICATION
    // =========================================================

    public Employee activateEmployee(UUID id) {

        Employee employee =
                employeeRepository
                        .findById(id)
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

        employee.setUpdatedOn(System.currentTimeMillis());

        return employeeRepository.save(employee);
    }


    // =========================================================
    // UPDATE EMPLOYEE PROFILE PHOTO
    // =========================================================

    public Employee updateEmployeeProfilePhoto(
            UUID id,
            MultipartFile profilePhoto) {

        Employee existingEmployee =
                employeeRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        if (profilePhoto == null
                || profilePhoto.isEmpty()) {

            throw new BadRequestException(
                    "Profile photo is required"
            );
        }

        // Validate profile photo
        validateProfilePhoto(profilePhoto);

        try {

            String extension =
                    getExtension(
                            profilePhoto.getOriginalFilename()
                    );

            String objectKey =
                    "employee-profile/"
                            + existingEmployee.getEmpId()
                            + "/profile-picture"
                            + extension;

            minioStorageService.upload(
                    permanentBucket,
                    objectKey,
                    profilePhoto.getBytes(),
                    profilePhoto.getContentType()
            );

            // Update object key in database
            existingEmployee.setProfilePhotoObjectKey(
                    objectKey
            );

            existingEmployee.setUpdatedOn(
                    System.currentTimeMillis()
            );

            return employeeRepository.save(
                    existingEmployee
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to update profile photo",
                    e
            );
        }
    }
}