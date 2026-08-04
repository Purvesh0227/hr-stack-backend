package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

}