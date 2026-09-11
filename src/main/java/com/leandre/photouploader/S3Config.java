package com.leandre.photouploader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    /**
     * Credentials come from the ECS task role and the region from the task
     * environment, so nothing is configured here. Traffic leaves through the
     * S3 gateway endpoint because the task subnets have no internet route.
     */
    @Bean
    S3Client s3Client() {
        // Initiate s3 client
        return S3Client.create();
    }
}
