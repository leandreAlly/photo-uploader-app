package com.leandre.photouploader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private S3Client s3;

    private StorageService storage;

    @BeforeEach
    void setUp() {
        storage = new StorageService(s3, "photos", "images/", "cdn.example.com");
    }

    @Test
    void storesJpegWithServerControlledKeyAndContentType() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "user-supplied.exe", "application/octet-stream",
                new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});

        String key = storage.store(file);

        assertThat(key).startsWith("images/").endsWith(".jpg");
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(request.getValue().contentDisposition()).isEqualTo("inline");
    }

    @Test
    void rejectsUnsupportedContentBeforeCallingS3() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.bin", "image/jpeg", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(IOException.class)
                .hasMessage("unsupported image format");

        verifyNoInteractions(s3);
    }

    @Test
    void rejectsFilesOverTenMegabytesBeforeCallingS3() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(IOException.class)
                .hasMessage("image size is outside the allowed range");

        verifyNoInteractions(s3);
    }
}
