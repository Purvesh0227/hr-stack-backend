package com.hrstack.hr_stack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TempFileStorageService {

    private final MinioStorageService minioStorageService;

    @Value("${minio.temp-bucket}")
    private String bucketName;

    public TempFileStorageService(
            MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    public String uploadTempFile(
            String fileName,
            byte[] fileBytes,
            String contentType) {

        String objectKey = "temp/" + fileName;

        minioStorageService.upload(
                bucketName,
                objectKey,
                fileBytes,
                contentType
        );

        return objectKey;
    }

    public byte[] downloadTempFile(String objectKey) {

        return minioStorageService.download(
                bucketName,
                objectKey
        );
    }

    public void deleteTempFile(String objectKey) {

        minioStorageService.delete(
                bucketName,
                objectKey
        );
    }
}