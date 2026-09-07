package com.hrstack.hr_stack.service;


import com.hrstack.hr_stack.entity.SalarySlip;
import com.hrstack.hr_stack.repository.SalarySlipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalarySlipStorageScheduler {
    private final SalarySlipRepository salarySlipRepository;
    private final MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    @Value("${minio.permanent-bucket}")
    private String permanentBucket;

    @Value("${salary-slip.replacement-window-months}")
    private long storageWindowMonths;

    public SalarySlipStorageScheduler(
            SalarySlipRepository salarySlipRepository,
            MinioStorageService minioStorageService) {

        this.salarySlipRepository = salarySlipRepository;
        this.minioStorageService = minioStorageService;
    }

//    @Scheduled(cron = "0 */2 * * * *")
    @Scheduled(cron = "0 0 0 * * *")
    public void moveExpiredSalarySlips() {

        List<SalarySlip> salarySlips =
                salarySlipRepository.findAll();

        long currentTime =
                System.currentTimeMillis();

        for (SalarySlip salarySlip : salarySlips) {

            Long generatedAt =
                    salarySlip.getGeneratedAt();

            if (generatedAt == null) {
                continue;
            }

            long storageWindow =
                    storageWindowMonths
                            * 30L
                            * 24
                            * 60
                            * 60
                            * 1000;

            long expiryTime =
                    generatedAt + storageWindow;

            // Salary slip has not expired yet
            if (currentTime < expiryTime) {
                continue;
            }

            String objectKey =
                    salarySlip.getPdfObjectKey();

            if (objectKey == null) {
                continue;
            }

            // Check if file exists in TEMP
            boolean existsInTemp =
                    minioStorageService.exists(
                            tempBucket,
                            objectKey
                    );

            if (!existsInTemp) {
                continue;
            }
            // Check if file already exists in PERMANENT
            boolean existsInPermanent =
                    minioStorageService.exists(
                            permanentBucket,
                            objectKey
                    );

            if (existsInPermanent) {

                // Remove duplicate TEMP file
                minioStorageService.delete(
                        tempBucket,
                        objectKey
                );

                continue;
            }
            // Move TEMP → PERMANENT
            minioStorageService.move(
                    tempBucket,
                    objectKey,
                    permanentBucket,
                    objectKey
            );
        }
    }
}
