package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TempToPermanentStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${s3.temp-bucket}")
    private String tempBucket;

    @Value("${s3.permanent-bucket}")
    private String permanentBucket;

    // Replacement allowed for 2 months
    @Value("${salary-slip.replacement-window-months}")
    private long replacementWindowMonths;

    public TempToPermanentStorageService(
            MinioStorageService minioStorageService) {

        this.minioStorageService = minioStorageService;
    }

    public String replaceSalarySlip(
            String tempObjectKey,
            String permanentObjectKey,
            Long generatedAt) {

        // Calculate replacement expiry time
        long currentTime = System.currentTimeMillis();

        long replacementWindow =
                replacementWindowMonths
                        * 30L
                        * 24
                        * 60
                        * 60
                        * 1000;

        long expiryTime =
                generatedAt + replacementWindow;

        // Check replacement window
        if (currentTime > expiryTime) {
            throw new RuntimeException(
                    "Salary slip replacement period has expired"
            );
        }

        // Verify replacement file exists in TEMP
        boolean tempFileExists =
                minioStorageService.exists(
                        tempBucket,
                        tempObjectKey
                );

        if (!tempFileExists) {
            throw new RuntimeException(
                    "Replacement file not found in temporary bucket"
            );
        }

        // Immediately move replacement file
        // TEMP → PERMANENT
        minioStorageService.move(
                tempBucket,
                tempObjectKey,
                permanentBucket,
                permanentObjectKey
        );

        // DB should point to permanent object
        return permanentObjectKey;
    }
}