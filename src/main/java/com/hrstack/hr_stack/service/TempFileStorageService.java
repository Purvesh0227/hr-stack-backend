package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TempFileStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String bucketName;

    public TempFileStorageService(
            MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    public String uploadSalarySlipToTemp(
            String empId,
            int month,
            int year,
            byte[] pdfBytes){
        String objectKey =
                "salary-slips/"
                +year
                +"/"
                +month
                +"/"
                +empId
                +".pdf";

        minioStorageService.upload(
                bucketName,
                objectKey,
                pdfBytes,
                "application/pdf");

        return objectKey;
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
}