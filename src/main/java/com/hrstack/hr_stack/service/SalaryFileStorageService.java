package com.hrstack.hr_stack.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class SalaryFileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public SalaryFileStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadSalarySlip(String empId, int month, int year, byte[] pdfBytes) {
        String objectKey = "salary-slips/" + year + "/" + month + "/" + empId + ".pdf";

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                            .contentType("application/pdf")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload salary slip to MinIO", e);
        }

        return objectKey;
    }

    public byte[] downloadSalarySlip(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch salary slip from MinIO", e);
        }
    }
}