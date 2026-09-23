package com.hrstack.hr_stack.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;

@Service
public class MinioStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public MinioStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public void upload(
            String bucketName,
            String objectKey,
            byte[] fileBytes,
            String contentType
    ) {
        try {

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to upload file to S3",
                    e
            );
        }
    }

    public byte[] download(
            String bucketName,
            String objectKey
    ) {
        try (
                InputStream inputStream =
                        s3Client.getObject(
                                GetObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(objectKey)
                                        .build()
                        )
        ) {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to download file from S3",
                    e
            );
        }
    }

    public void delete(
            String bucketName,
            String objectKey
    ) {
        try {

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to delete file from S3",
                    e
            );
        }
    }

    public void move(
            String sourceBucket,
            String sourceObjectKey,
            String destinationBucket,
            String destinationObjectKey
    ) {
        try {

            String copySource =
                    sourceBucket + "/" + sourceObjectKey;

            s3Client.copyObject(
                    CopyObjectRequest.builder()
                            .copySource(copySource)
                            .destinationBucket(destinationBucket)
                            .destinationKey(destinationObjectKey)
                            .build()
            );

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(sourceBucket)
                            .key(sourceObjectKey)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to move file from S3",
                    e
            );
        }
    }

    public String getSignedUrl(
            String bucketName,
            String objectKey
    ) {
        try {

            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();

            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(
                                    Duration.ofHours(1)
                            )
                            .getObjectRequest(getObjectRequest)
                            .build();

            return s3Presigner
                    .presignGetObject(presignRequest)
                    .url()
                    .toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate signed URL",
                    e
            );
        }
    }

    public boolean exists(
            String bucketName,
            String objectKey
    ) {
        try {

            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build()
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public String getPresignedUploadUrl(
            String bucketName,
            String objectKey
    ) {
        try {

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();

            PutObjectPresignRequest presignRequest =
                    PutObjectPresignRequest.builder()
                            .signatureDuration(
                                    Duration.ofMinutes(15)
                            )
                            .putObjectRequest(putObjectRequest)
                            .build();

            return s3Presigner
                    .presignPutObject(presignRequest)
                    .url()
                    .toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate upload URL",
                    e
            );
        }
    }
}