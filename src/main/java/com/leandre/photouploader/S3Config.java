package com.leandre.photouploader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Bean
    S3Client s3Client() {
        // Initiate s3 client
        return S3Client.create();
    }
}
