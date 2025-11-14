package com.passmais.application.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.passmais.infrastructure.config.properties.AwsProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3StorageService {

    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final String region;

    public S3StorageService(AmazonS3 amazonS3, AwsProperties awsProperties) {
        this.amazonS3 = amazonS3;
        this.bucketName = awsProperties.getS3().getBucket();
        this.region = awsProperties.getRegion();

        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("AWS_S3_BUCKET is not configured. Please define it in the .env file or environment variables.");
        }
    }

    public String uploadDoctorPhoto(MultipartFile file, UUID doctorProfileId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Imagem inválida: nenhum arquivo foi enviado.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String key = String.format("doctors/%s/%s%s", doctorProfileId, UUID.randomUUID(), extension);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            amazonS3.putObject(request);
        }

        return generatePublicUrl(key);
    }

    public String generatePublicUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
