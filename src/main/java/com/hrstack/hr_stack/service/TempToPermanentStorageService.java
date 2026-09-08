package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TempToPermanentStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    @Value("${minio.permanent-bucket}")
    private String permanentBucket;

    // 2 months ≈ 60 days
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

        long currentTime = System.currentTimeMillis();

        long replacementWindow =
                replacementWindowMonths
                        * 30L
                        * 24
                        * 60
                        * 60
                        * 1000;

        long expiryTime = generatedAt + replacementWindow;

        if (currentTime > expiryTime) {
            throw new RuntimeException(
                    "Salary slip replacement period has expired"
            );
        }

        // New replacement file must exist in TEMP
        if (!minioStorageService.exists(tempBucket, tempObjectKey)) {
            throw new RuntimeException(
                    "Replacement file not found in temporary bucket"
            );
        }

        // Check where the current salary slip exists
        boolean permanentFileExists =
                minioStorageService.exists(
                        permanentBucket,
                        permanentObjectKey
                );

        if (permanentFileExists) {

            // Existing slip is permanent → replace it
            minioStorageService.move(
                    tempBucket,
                    tempObjectKey,
                    permanentBucket,
                    permanentObjectKey
            );

            return permanentObjectKey;
        }

        // Existing slip is still temporary.
        // The new upload already overwrote the same TEMP object.
        return tempObjectKey;
    }
}