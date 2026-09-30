package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.NotificationSubscription;
import com.hrstack.hr_stack.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSubscriptionRepository
        extends JpaRepository<NotificationSubscription, UUID> {

    Optional<NotificationSubscription> findByEndpoint(String endpoint);

    List<NotificationSubscription> findByEmployee(Employee employee);

    void deleteByEndpoint(String endpoint);
}