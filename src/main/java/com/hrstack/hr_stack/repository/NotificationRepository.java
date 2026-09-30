package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    List<Notification> findByEmployeeOrderByCreatedOnDesc(
            Employee employee
    );

    List<Notification> findByEmployeeAndReadFalseOrderByCreatedOnDesc(
            Employee employee
    );

    long countByEmployeeAndReadFalse(Employee employee);
}