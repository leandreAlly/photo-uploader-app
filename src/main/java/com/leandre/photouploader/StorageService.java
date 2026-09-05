package com.leandre.photouploader;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Writes uploads to the private S3 bucket and turns an object key into the
 * CloudFront URL a browser can actually fetch. The bucket itself is not
 * readable - only the distribution's origin access control can read it.
 */
@Service
public class StorageService {

    private final S3Client s3;
    private final String bucket;
    private final String prefix;
    private final String cdnDomain;

    public StorageService(
            S3Client s3,
            @Value("${app.image.bucket}") String bucket,
            @Value("${app.image.prefix:images/}") String prefix,
            @Value("${app.cdn.domain}") String cdnDomain) {
        this.s3 = s3;
        this.bucket = bucket;
        this.prefix = prefix;
        this.cdnDomain = cdnDomain;
    }

    public String store(MultipartFile file) throws IOException {
        String key = prefix + UUID.randomUUID() + extensionOf(file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return key;
    }

    public String urlFor(String objectKey) {
        return "https://" + cdnDomain + "/" + objectKey;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
