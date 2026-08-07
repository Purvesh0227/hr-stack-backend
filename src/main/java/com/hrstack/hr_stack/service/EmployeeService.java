package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Employee registerEmployee(Employee employee) {
        if (employeeRepository.existsByEmailIgnoreCase(employee.getEmail())) {
            throw new RuntimeException("Email already exists. Please use another email.");
        }

        employee.setRole("EMPLOYEE");

        employee.setPassword(encoder.encode(employee.getPassword()));

        return employeeRepository.save(employee);
    }

    //create admin

    public Employee createAdmin(Employee employee){
        if(employeeRepository.existsByEmailIgnoreCase(employee.getEmail())){
            throw new RuntimeException("Email already exists. Please use another email.");
        }
        employee.setPassword(encoder.encode(employee.getPassword()));
        employee.setRole("ADMIN");

        return employeeRepository.save(employee);
    }

    public Employee login(String email, String password){
        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("Enter Valid Email")
                );

        if(!encoder.matches(password, employee.getPassword())){
            throw new RuntimeException("Enter VAlid Password");
        }
        return employee;
    }




    public List<Employee> getAllEmployees(String email){

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        if(!"ADMIN".equalsIgnoreCase(employee.getRole())){
            throw new RuntimeException("Access Denied . You are not Admin");
        }

        return employeeRepository.findByRoleIgnoreCase("EMPLOYEE");
    }

    //get all admins

    public List<Employee> getAllAdmins(String email){
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        if(!"ADMIN".equalsIgnoreCase(employee.getRole())){
            throw new RuntimeException("Access Denied . You are not Admin");
        }
        return employeeRepository.findByRoleIgnoreCase("ADMIN");
    }

    public Employee getAdminProfile(String email){
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin Not Found"));
        if(!"ADMIN".equalsIgnoreCase(employee.getRole())){
            throw new RuntimeException("Access Denied . You are not Admin");
        }
        return employee;
    }

    // finding employee detailsby email (task2)
    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee with this email found"));
    }

    //get emp by uuid(task3)
    public Employee getEmployeeById(UUID id){
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    //delete emp by uuid
    public void deleteEmployee(UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }



}