package com.tejas.metlife.claimprocessor.service;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class BlobStorageService {

    private final BlobContainerClient containerClient;

    public BlobStorageService(
        @Value("${azure.storage.connection-string}") String connectionString,
        @Value("${azure.storage.container-name}") String containerName
    ) {
        BlobServiceClient blobServiceClient =
                new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            InputStream is = file.getInputStream();
            blobClient.upload(is, file.getSize(), true);

            blobClient.setHttpHeaders(
                new BlobHttpHeaders().setContentType(file.getContentType())
            );

            return blobClient.getBlobUrl(); // ✅ return public URL
        }
        catch (Exception e) {
            throw new RuntimeException("File upload failed");
        }
    }
}
