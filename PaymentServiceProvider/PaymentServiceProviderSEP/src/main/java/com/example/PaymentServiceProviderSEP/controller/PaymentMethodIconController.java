package com.example.PaymentServiceProviderSEP.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-methods/icon")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodIconController {

    @Value("${app.upload.dir:uploads/payment-method-icons}")
    private String uploadDir;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadIcon(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File must be an image"));
            }

            // Validate file size (max 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File size must be less than 2MB"));
            }

            // Create upload directory with absolute path if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            log.info("Upload path: {}", uploadPath);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : ".png";
            String uniqueFileName = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFileName);
            log.info("Saving file to: {}", filePath);
            file.transferTo(filePath.toFile());

            log.info("Icon uploaded successfully: {}", uniqueFileName);

            // Return response with filename
            Map<String, Object> response = new HashMap<>();
            response.put("filename", uniqueFileName);
            response.put("originalName", originalFileName);
            response.put("size", file.getSize());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getIcon(@PathVariable String filename) {
        try {
            // Validate filename to prevent directory traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            File file = filePath.toFile();
            log.debug("Serving icon from: {}", filePath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // Determine media type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            log.error("Error retrieving icon", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<?> deleteIcon(@PathVariable String filename) {
        try {
            // Validate filename
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid filename"));
            }

            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            if (file.delete()) {
                log.info("Icon deleted: {}", filename);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("Failed to delete file"));
            }

        } catch (Exception e) {
            log.error("Error deleting icon", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete file: " + e.getMessage()));
        }
    }

    static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
