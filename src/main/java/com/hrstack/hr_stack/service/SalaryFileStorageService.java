package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SalaryFileStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${minio.permanent-bucket}")
    private String bucketName;

    public SalaryFileStorageService(
            MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    // Upload salary slip
    public String uploadSalarySlip(
            String empId,
            int month,
            int year,
            byte[] pdfBytes) {

        String objectKey =
                "salary-slips/"
                        + year
                        + "/"
                        + month
                        + "/"
                        + empId
                        + ".pdf";

        minioStorageService.upload(
                bucketName,
                objectKey,
                pdfBytes,
                "application/pdf"
        );

        return objectKey;
    }

    // Download salary slip
    public byte[] downloadSalarySlip(String objectKey) {

        return minioStorageService.download(
                bucketName,
                objectKey
        );
    }

    // Delete salary slip
    public void deleteSalarySlip(String objectKey) {

        minioStorageService.delete(
                bucketName,
                objectKey
        );
    }

    //signed url
    public String getSalarySlipSignedUrl(String objectKey) {
        return minioStorageService.getSignedUrl(
                bucketName,
                objectKey
        );
    }
}

