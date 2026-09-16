package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.hrstack.hr_stack.util.SalaryObjectKeyUtil;

@Service
public class SalaryFileStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${minio.permanent-bucket}")
    private String permanentBucket;

    @Value("${minio.temp-bucket}")
    private String tempBucket;

    public SalaryFileStorageService(
            MinioStorageService minioStorageService) {

        this.minioStorageService = minioStorageService;
    }

    // Upload salary slip to permanent bucket
    public String uploadSalarySlip(
            String empId,
            int month,
            int year,
            byte[] pdfBytes) {

        String objectKey =
                SalaryObjectKeyUtil.buildSalarySlipObjectKey(
                        empId,
                        month,
                        year
                );

        minioStorageService.upload(
                permanentBucket,
                objectKey,
                pdfBytes,
                "application/pdf"
        );

        return objectKey;
    }

    // Download salary slip from permanent bucket
    public byte[] downloadSalarySlip(String objectKey) {

        return minioStorageService.download(
                permanentBucket,
                objectKey
        );
    }

    // Delete salary slip from permanent bucket
    public void deleteSalarySlip(String objectKey) {

        minioStorageService.delete(
                permanentBucket,
                objectKey
        );
    }

    // Generate signed URL from permanent bucket
    public String getSalarySlipSignedUrl(String objectKey) {

        return minioStorageService.getSignedUrl(
                permanentBucket,
                objectKey
        );
    }

    // Download salary slip from TEMP or PERMANENT bucket
    public byte[] downloadSalarySlipFromEitherBucket(
            String objectKey) {

        if (minioStorageService.exists(
                tempBucket,
                objectKey)) {

            return minioStorageService.download(
                    tempBucket,
                    objectKey
            );
        }

        if (minioStorageService.exists(
                permanentBucket,
                objectKey)) {

            return minioStorageService.download(
                    permanentBucket,
                    objectKey
            );
        }

        throw new RuntimeException(
                "Salary slip file not found"
        );
    }

    // Generate signed URL from TEMP or PERMANENT bucket
    public String getSalarySlipSignedUrlFromEitherBucket(
            String objectKey) {

        if (minioStorageService.exists(
                tempBucket,
                objectKey)) {

            return minioStorageService.getSignedUrl(
                    tempBucket,
                    objectKey
            );
        }

        if (minioStorageService.exists(
                permanentBucket,
                objectKey)) {

            return minioStorageService.getSignedUrl(
                    permanentBucket,
                    objectKey
            );
        }

        throw new RuntimeException(
                "Salary slip file not found"
        );
    }
}