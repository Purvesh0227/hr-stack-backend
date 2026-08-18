package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.entity.SalarySlip;
import com.hrstack.hr_stack.entity.SalaryStructure;
import com.hrstack.hr_stack.service.SalaryCalculationService;
import com.hrstack.hr_stack.service.SalaryStructureService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/salary")
@SecurityRequirement(name = "bearerAuth")
public class SalaryController {

    private final SalaryCalculationService  salaryCalculationService;
    private final SalaryStructureService salaryStructureService;

    public SalaryController(SalaryCalculationService salaryCalculationService,
                            SalaryStructureService salaryStructureService) {
        this.salaryCalculationService = salaryCalculationService;
        this.salaryStructureService = salaryStructureService;
    }

    @PostMapping("/structure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalaryStructure> createOrUpdateSalaryStructure(
            @RequestBody SalaryStructure salaryStructure) {

        SalaryStructure savedStructure =
                salaryStructureService.createOrUpdateSalaryStructure(
                        salaryStructure
                );

        return ResponseEntity.ok(savedStructure);
    }

    @GetMapping("/structure/{empId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalaryStructure> getSalaryStructure(
            @PathVariable String empId) {

        return ResponseEntity.ok(
                salaryStructureService.getSalaryStructure(empId)
        );
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalarySlip> generateSalary(
            @RequestParam String empId,
            @RequestParam int month,
            @RequestParam int year) {

        SalarySlip salarySlip =
                salaryCalculationService.generateSalary(
                        empId,
                        month,
                        year
                );

        return ResponseEntity.ok(salarySlip);
    }

    @GetMapping("/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<byte[]> downloadSalarySlip(
            @RequestParam String empId,
            @RequestParam int month,
            @RequestParam int year) {

        SalarySlip salarySlip =
                salaryCalculationService.getSalarySlip(
                        empId, month, year
                );

        return ResponseEntity.ok()
                .header(
                        "Content-Disposition",
                        "attachment; filename=salary-slip-" + empId + "-" + month + "-" + year + ".pdf"
                )
                .header("Content-Type", "application/pdf")
                .body(salarySlip.getPdfData());
    }

    @GetMapping("/view")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<SalarySlip>> viewSalarySlips(
            Authentication authentication,
            @RequestParam(defaultValue = "MY") String scope) {

        String email = authentication.getName();

        List<SalarySlip> salarySlips =
                salaryCalculationService.viewSalarySlips(
                        email,
                        scope
                );

        return ResponseEntity.ok(salarySlips);
    }


}