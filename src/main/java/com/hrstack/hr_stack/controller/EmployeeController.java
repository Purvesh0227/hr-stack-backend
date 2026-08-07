package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.LoginRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    //to register employee
    @PostMapping("/register")
    public ResponseEntity<?> registerEmployee(@Valid @RequestBody Employee employee) {
        try {

            Employee savedEmployee = employeeService.registerEmployee(employee);
            return ResponseEntity.ok(savedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    //create admin

    @PostMapping("/createAdmin")
    public Employee  createAdmin(@Valid @RequestBody Employee employee) {
        return employeeService.createAdmin(employee);
    }

    // to get all emplyee details
    @GetMapping("/allEmployees")
    public List<Employee> getAllEmployees(@RequestParam String email) {

        return employeeService.getAllEmployees(email);

    }

    @GetMapping("/allAdmins")
    public List<Employee> getAllAdmins(@RequestParam String email){
        return employeeService.getAllAdmins(email);
    }

    @GetMapping("/adminProfile")
    public Employee getAdminProfile(@RequestParam String email){
        return employeeService.getAdminProfile(email);
    }

    @PostMapping("/login")
    public Employee login(@RequestBody LoginRequest request) {
        return employeeService.login(
                request.getEmail(),
                request.getPassword()
        );
    }

    // to get details of employee by email(task2)
    @GetMapping("/email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {

        return employeeService.getEmployeeByEmail(email);
    }

    //to get emp by uuid
    @GetMapping("/{uuid}")
    public Employee getEmployeeById(@PathVariable UUID uuid) {
        return employeeService.getEmployeeById(uuid);
    }

    //to delete emp by uuid
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable UUID uuid) {
        employeeService.deleteEmployee(uuid);
        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
    }
}