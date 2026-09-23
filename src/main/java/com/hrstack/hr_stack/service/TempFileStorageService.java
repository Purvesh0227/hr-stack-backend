package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.util.SalaryObjectKeyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TempFileStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${s3.temp-bucket}")
    private String bucketName;

    public TempFileStorageService(
            MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    public byte[] downloadTempFile(
            String objectKey) {

        return minioStorageService.download(
                bucketName,
                objectKey
        );
    }

    public void deleteTempFile(
            String objectKey) {

        minioStorageService.delete(
                bucketName,
                objectKey
        );
    }

    public boolean tempFileExists(
            String objectKey) {

        return minioStorageService.exists(
                bucketName,
                objectKey
        );
    }

    public String generateUploadUrl(
            String empId,
            int month,
            int year) {

        String objectKey =
                SalaryObjectKeyUtil.buildSalarySlipObjectKey(
                        empId,
                        month,
                        year
                );

        return minioStorageService.getPresignedUploadUrl(
                bucketName,
                objectKey
        );
    }
    public String uploadSalarySlipToTemp(
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
                bucketName,
                objectKey,
                pdfBytes,
                "application/pdf"
        );

        return objectKey;
    }
}