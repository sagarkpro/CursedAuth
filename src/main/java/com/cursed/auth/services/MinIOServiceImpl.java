package com.cursed.auth.services;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class MinIOServiceImpl implements MinIOService {
    private final S3Client client;
    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-endpoint}")
    private String minIOEndpoint;

    public MinIOServiceImpl(S3Client client) {
        this.client = client;
    }

    @Override
    public String upload(MultipartFile file, String folder) throws IOException {

        String fileName = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10)
                + "-"
                + file.getOriginalFilename();

        String key = folder + "/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        client.putObject(
                request,
                RequestBody.fromBytes(
                        file.getBytes()));

        return minIOEndpoint
                + "/"
                + bucket
                + "/"
                + key;
    }
}
