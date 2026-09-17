package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.EmployeeDocument;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeDocumentRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.hrstack.hr_stack.dto.EmployeeDocumentUploadResponse;

import java.util.UUID;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;

import java.util.UUID;

@Service
public class EmployeeDocumentService {

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    @Value("${minio.permanent-bucket}")
    private String permanentBucket;

    public EmployeeDocument getDocumentsByEmployeeId(UUID employeeId) {

        return employeeDocumentRepository
                .findByEmployeeId(employeeId)
                .orElse(null);
    }

    public EmployeeDocument saveDocuments(
            UUID employeeId,
            String idProofType,
            String idProofNumber,
            String idProofObjectKey,
            String addressProofType,
            String addressProofNumber,
            String addressProofObjectKey) {

        validateEmployeeOwnership(employeeId);

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        String expectedPrefix = "employee-documents/" + employee.getEmpId() + "/";

        if(!idProofObjectKey.startsWith(expectedPrefix)){
            throw new BadRequestException("Invalid ID proof path");
        }

        if(!addressProofObjectKey.startsWith(expectedPrefix)){
            throw new BadRequestException("Invalid Address proof path");
        }


        if (!"PENDING_VERIFICATION".equalsIgnoreCase(
                employee.getStatus().replace(" ", "_"))) {

            throw new BadRequestException(
                    "Document can only be submitted during verification"
            );
        }

        if(!"AADHAAR".equalsIgnoreCase(idProofType)
                && !"PAN".equalsIgnoreCase(idProofType)){
            throw new BadRequestException("Id proof must be aadhaar or pan");
        }

        if(!"AADHAAR".equalsIgnoreCase(addressProofType)
            && !"LIGHT_BILL".equalsIgnoreCase(addressProofType)){
            throw new BadRequestException("Address proof must be aadhaar or light bill");
        }

        if(idProofNumber == null || idProofNumber.isBlank()){
            throw new BadRequestException("ID prood number is required");
        }

        if(addressProofNumber == null || addressProofNumber.isBlank()){
            throw new BadRequestException("Address proof number is required");
        }

        if(idProofObjectKey == null || idProofObjectKey.isBlank()){
            throw new BadRequestException("ID proof File is required");
        }

        if(addressProofObjectKey == null || addressProofObjectKey.isBlank()){
            throw new BadRequestException("Address proof File is required");
        }

        if(!minioStorageService.exists(tempBucket,idProofObjectKey)){
            throw new BadRequestException("Id proof file not found in temporary storage");
        }

        if(!minioStorageService.exists(tempBucket,addressProofObjectKey)){
            throw new BadRequestException("Address proof file not found in temporary storage");
        }


        EmployeeDocument documents =
                employeeDocumentRepository
                        .findByEmployeeId(employeeId)
                        .orElse(new EmployeeDocument());

        documents.setEmployee(employee);

        documents.setIdProofType(idProofType.toUpperCase());
        documents.setIdProofNumber(idProofNumber);
        documents.setIdProofObjectKey(idProofObjectKey);

        documents.setAddressProofType(addressProofType.toUpperCase());
        documents.setAddressProofNumber(addressProofNumber);
        documents.setAddressProofObjectKey(addressProofObjectKey);

        return employeeDocumentRepository.save(documents);
    }

    public EmployeeDocumentUploadResponse generateDocumentUploadUrl(
            UUID employeeId,
            String documentType) {
        validateEmployeeOwnership(employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        String objectKey =
                "employee-documents/"
                        + employee.getEmpId()
                        + "/"
                        + documentType.toLowerCase()
                        + ".pdf";

        String uploadUrl = minioStorageService.getPresignedUploadUrl(
                tempBucket,
                objectKey);

        return new EmployeeDocumentUploadResponse(uploadUrl, objectKey
        );
    }

    private void validateEmployeeOwnership(UUID employeeId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("Employee authentication required");
        }

        String loggedInEmail = authentication.getName();

        Employee loggedInEmployee =
                employeeRepository.findByEmail(loggedInEmail)
                        .orElseThrow(() -> new BadRequestException("Authenticated employee not found"));

        if (!loggedInEmployee.getId().equals(employeeId)) {
            throw new BadRequestException(
                    "You can upload documents only for your own account"
            );
        }
    }

    public String getDocumentViewUrl(
            UUID employeeId,
            String documentType) {

        EmployeeDocument documents =
                employeeDocumentRepository
                        .findByEmployeeId(employeeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee documents not found"
                                )
                        );

        String objectKey;

        if ("ID_PROOF".equalsIgnoreCase(documentType)) {

            objectKey = documents.getIdProofObjectKey();

        } else if ("ADDRESS_PROOF".equalsIgnoreCase(documentType)) {

            objectKey = documents.getAddressProofObjectKey();

        } else {

            throw new BadRequestException(
                    "Invalid document type"
            );
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new BadRequestException(
                    "Document file not found"
            );
        }

        String bucket;

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        if ("ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            bucket = permanentBucket;
        } else {
            bucket = tempBucket;
        }

        if (!minioStorageService.exists(bucket, objectKey)) {
            throw new BadRequestException(
                    "Document file not found in storage"
            );
        }

        return minioStorageService.getSignedUrl(
                bucket,
                objectKey
        );
    }
    public Employee getEmployeeById(UUID uuid) {

        return employeeRepository
                .findById(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + uuid
                        ));
    }
}