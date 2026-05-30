package com.cursed.auth.services;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface MinIOService {
    String upload(MultipartFile file, String folder) throws IOException;

    boolean delete(String filePath);
}