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

    public TempToPermanentStorageService(
            MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    public String moveToPermanent(
            String tempObjectKey,
            String permanentObjectKey) {

        minioStorageService.move(
                tempBucket,
                tempObjectKey,
                permanentBucket,
                permanentObjectKey
        );

        return permanentObjectKey;
    }
}