package com.hrstack.hr_stack.controller;


import com.hrstack.hr_stack.dto.EmployeeDocumentRequest;
import com.hrstack.hr_stack.entity.EmployeeDocument;
import com.hrstack.hr_stack.service.EmployeeDocumentService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hrstack.hr_stack.dto.EmployeeDocumentUploadResponse;
import java.util.UUID;

@RestController
@RequestMapping("/employee/{uuid}/documents")
@Tag(name = "Employee Documents")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeDocumentController {
    @Autowired
    private EmployeeDocumentService employeeDocumentService;

    //generate minio upload url

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Hidden
    @GetMapping("/upload-url")
    public ResponseEntity<EmployeeDocumentUploadResponse> generateUploadUrl(@PathVariable UUID  uuid , @RequestParam String documentType) {

        if(!documentType.equalsIgnoreCase("ID_PROOF")
                && !documentType.equalsIgnoreCase("ADDRESS_PROOF")){

            return ResponseEntity.badRequest()
                    .build();
        }

        EmployeeDocumentUploadResponse response =
                employeeDocumentService.generateDocumentUploadUrl(
                        uuid,
                        documentType
                );
        return ResponseEntity.ok(response);
    }


    //save doc details

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Hidden
    @PostMapping
    public ResponseEntity<EmployeeDocument> saveDocuments(
            @PathVariable UUID uuid , @RequestBody EmployeeDocumentRequest request){
        EmployeeDocument document =
                employeeDocumentService.saveDocuments(
                        uuid,
                        request.getIdProofType(),
                        request.getIdProofNumber(),
                        request.getIdProofObjectKey(),
                        request.getAddressProofType(),
                        request.getAddressProofNumber(),
                        request.getAddressProofObjectKey()
                );

        return ResponseEntity.ok(document);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Hidden
    @GetMapping("/view-url")
    public ResponseEntity<String> getDocumentViewUrl(
            @PathVariable UUID uuid,
            @RequestParam String documentType) {

        String url =
                employeeDocumentService.getDocumentViewUrl(
                        uuid,
                        documentType
                );

        return ResponseEntity.ok(url);
    }
}
