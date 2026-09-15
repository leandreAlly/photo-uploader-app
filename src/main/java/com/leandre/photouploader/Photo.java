package com.leandre.photouploader;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String objectKey;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Photo() {
    }

    public Photo(String objectKey, String description) {
        this.objectKey = objectKey;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
