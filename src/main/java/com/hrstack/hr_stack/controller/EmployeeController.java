package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.LoginRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    //to register employee
    @PostMapping("/register")
    public Employee registerEmployee(@RequestBody Employee employee) {
        return employeeService.registerEmployee(employee);
    }

    // to get all emplyee details
    @GetMapping("/all")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @PostMapping("/login")
    public Employee login(@RequestBody LoginRequest request) {
        return employeeService.login(
                request.getEmail(),
                request.getPassword()
        );
    }

    // to get details of employee by email
    @GetMapping("/email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
        return employeeService.getEmployeeByEmail(email);
    }


}