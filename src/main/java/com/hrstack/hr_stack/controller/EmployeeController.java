package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.LoginRequest;
import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.security.JwtService;
import com.hrstack.hr_stack.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hrstack.hr_stack.dto.RegisterEmployeeRequest;
import com.hrstack.hr_stack.dto.EmployeeProfileResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@RestController
@RequestMapping("/employee")
@Tag(name = "Employee")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JwtService  jwtService;

    //to register employee
    @PostMapping(
            value = "/register",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> registerEmployee(
            @Valid @ModelAttribute RegisterEmployeeRequest request) {
        try {
            Employee savedEmployee = employeeService.registerEmployee(request);
            return ResponseEntity.ok(savedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    //create admin

    @PostMapping("/createAdmin")
    public ResponseEntity<?> createAdmin(
            @Valid @RequestBody Employee employee) {
        try {
            Employee savedAdmin = employeeService.createAdmin(employee);
            return ResponseEntity.ok(savedAdmin);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // to get all emplyee details
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/allEmployees")
    public List<Employee> getAllEmployees(@RequestParam String email) {

        return employeeService.getAllEmployees(email);

    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/allAdmins")
    public List<Employee> getAllAdmins(@RequestParam String email){
        return employeeService.getAllAdmins(email);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/adminProfile")
    public Employee getAdminProfile(@RequestParam String email){
        return employeeService.getAdminProfile(email);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        Employee employee = employeeService.login(
                request.getEmail(),
                request.getPassword()
        );

        String token = jwtService.generateToken(
                employee.getEmail(),
                employee.getRole()
        );

        LoginResponse response =
                new LoginResponse(token, employee);

        return ResponseEntity.ok(response);
    }


    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {

        return employeeService.getEmployeeByEmail(email);
    }

    //to get emp by uuid
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{uuid}")
    public EmployeeProfileResponse getEmployeeById(@PathVariable UUID uuid) {
        return employeeService.getEmployeeById(uuid);
    }

    // Update employee details
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{uuid}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable UUID uuid,
            @RequestBody Employee employee) {

        Employee updatedEmployee = employeeService.updateEmployee(uuid, employee);

        return ResponseEntity.ok(updatedEmployee);
    }

    //to delete emp by uuid
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable UUID uuid) {
        employeeService.deleteEmployee(uuid);
        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
    }

    //request doc from emp
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{uuid}/request-documents")
    public ResponseEntity<Employee> requestDocuments(
            @PathVariable UUID uuid,
            Authentication authentication) {

        System.out.println("===== REQUEST DOCUMENTS =====");
        System.out.println("AUTH USER = " + authentication.getName());
        System.out.println("AUTHORITIES = " + authentication.getAuthorities());

        Employee employee =
                employeeService.requestDocuments(uuid);

        return ResponseEntity.ok(employee);
    }

    //activate employee

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{uuid}/activate")
    public ResponseEntity<Employee> activateEmployee(@PathVariable UUID uuid){
        Employee employee = employeeService.activateEmployee(uuid);
        return ResponseEntity.ok(employee);
    }

}