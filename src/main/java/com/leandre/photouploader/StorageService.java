package com.leandre.photouploader;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
        if (file.getSize() <= 0 || file.getSize() > 10L * 1024 * 1024) {
            throw new IOException("image size is outside the allowed range");
        }

        byte[] content = file.getBytes();
        ImageType imageType = ImageType.detect(content);
        if (imageType == null) {
            throw new IOException("unsupported image format");
        }

        String key = prefix + UUID.randomUUID() + imageType.extension();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(imageType.contentType())
                .contentDisposition("inline")
                .build();

        s3.putObject(request, RequestBody.fromBytes(content));
        return key;
    }

    public void delete(String objectKey) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }

    public String urlFor(String objectKey) {
        return "https://" + cdnDomain + "/" + objectKey;
    }

    private enum ImageType {
        JPEG(".jpg", "image/jpeg"),
        PNG(".png", "image/png"),
        GIF(".gif", "image/gif"),
        WEBP(".webp", "image/webp");

        private final String extension;
        private final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        String extension() {
            return extension;
        }

        String contentType() {
            return contentType;
        }

        static ImageType detect(byte[] content) {
            if (content.length >= 3
                    && (content[0] & 0xff) == 0xff
                    && (content[1] & 0xff) == 0xd8
                    && (content[2] & 0xff) == 0xff) {
                return JPEG;
            }
            if (content.length >= 8
                    && (content[0] & 0xff) == 0x89
                    && content[1] == 'P'
                    && content[2] == 'N'
                    && content[3] == 'G'
                    && (content[4] & 0xff) == 0x0d
                    && (content[5] & 0xff) == 0x0a
                    && (content[6] & 0xff) == 0x1a
                    && (content[7] & 0xff) == 0x0a) {
                return PNG;
            }
            if (content.length >= 6
                    && ((content[0] == 'G' && content[1] == 'I' && content[2] == 'F')
                    && (content[3] == '8' && (content[4] == '7' || content[4] == '9') && content[5] == 'a'))) {
                return GIF;
            }
            if (content.length >= 12
                    && content[0] == 'R'
                    && content[1] == 'I'
                    && content[2] == 'F'
                    && content[3] == 'F'
                    && content[8] == 'W'
                    && content[9] == 'E'
                    && content[10] == 'B'
                    && content[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
