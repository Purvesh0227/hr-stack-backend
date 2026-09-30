package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.PushSubscriptionRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.NotificationSubscription;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.NotificationSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSubscriptionService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final EmployeeRepository employeeRepository;

    public NotificationSubscriptionService(
            NotificationSubscriptionRepository subscriptionRepository,
            EmployeeRepository employeeRepository) {

        this.subscriptionRepository = subscriptionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public void subscribe(
            String email,
            PushSubscriptionRequest request) {

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        NotificationSubscription subscription =
                subscriptionRepository
                        .findByEndpoint(request.getEndpoint())
                        .orElseGet(NotificationSubscription::new);

        long currentTime = System.currentTimeMillis();

        subscription.setEmployee(employee);
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());

        if (subscription.getCreatedOn() == null) {
            subscription.setCreatedOn(currentTime);
        }

        subscription.setUpdatedOn(currentTime);

        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(
            String email,
            String endpoint) {

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found")
                );

        NotificationSubscription subscription =
                subscriptionRepository
                        .findByEndpoint(endpoint)
                        .orElse(null);

        if (subscription == null) {
            return;
        }

        if (!subscription.getEmployee().getId()
                .equals(employee.getId())) {

            throw new RuntimeException(
                    "Subscription does not belong to employee"
            );
        }

        subscriptionRepository.delete(subscription);
    }
}