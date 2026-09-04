package com.hrstack.hr_stack.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class MinioStorageService {

    private final MinioClient minioClient;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // Upload file to any bucket
    public void upload(
            String bucketName,
            String objectKey,
            byte[] fileBytes,
            String contentType) {

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(
                                    new ByteArrayInputStream(fileBytes),
                                    fileBytes.length,
                                    -1
                            )
                            .contentType(contentType)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to upload file to MinIO", e
            );
        }
    }

    // Download file from any bucket
    public byte[] download(
            String bucketName,
            String objectKey) {

        try (
                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .build()
                )
        ) {
            return stream.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to download file from MinIO", e
            );
        }
    }

    // Delete file from any bucket
    public void delete(
            String bucketName,
            String objectKey) {

        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to delete file from MinIO", e
            );
        }
    }

    public void move(
            String sourceBucket,
            String sourceObjectKey,
            String destinationBucket,
            String destinationObjectKey){

        try{
            minioClient.copyObject(
                    io.minio.CopyObjectArgs.builder()
                            .bucket(destinationBucket)
                            .object(destinationObjectKey)
                            .source(
                                    io.minio.CopySource.builder()
                                            .bucket(sourceBucket)
                                            .object(sourceObjectKey)
                                            .build()
                            )
                            .build());

            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(sourceBucket)
                            .object(sourceObjectKey)
                            .build() );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to move file from MinIO", e
            );
        }
    }

    public String getSignedUrl(String bucketName,
                               String objectKey) {
        try{
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(60 * 60)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed To Generate Signed Url "+e);
        }
    }
}

