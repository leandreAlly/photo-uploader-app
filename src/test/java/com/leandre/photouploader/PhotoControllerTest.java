package com.leandre.photouploader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slices the web layer only: no datasource, so the suite runs in CI without a
 * database or AWS credentials.
 */
@WebMvcTest(PhotoController.class)
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoRepository photos;

    @MockitoBean
    private StorageService storage;

    @Test
    void healthReportsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void versionEndpointExposesBuildMetadata() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.commit").exists())
                .andExpect(jsonPath("$.builtAt").exists());
    }

    @Test
    void galleryRendersStoredPhotosAsCdnUrls() throws Exception {
        given(photos.findAllByOrderByCreatedAtDesc())
                .willReturn(List.of(new Photo("images/abc.jpg", "a cat")));
        given(storage.urlFor("images/abc.jpg"))
                .willReturn("https://d123.cloudfront.net/images/abc.jpg");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("photos"));
    }

    @Test
    void uploadStoresTheFileAndRedirects() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cat.jpg", "image/jpeg", "not-really-a-jpeg".getBytes());

        given(storage.store(any())).willReturn("images/generated.jpg");

        mockMvc.perform(multipart("/upload").file(file).param("description", "a cat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        Mockito.verify(photos).save(any(Photo.class));
    }

    @Test
    void emptyUploadIsRejectedWithoutTouchingStorage() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "none.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/upload").file(empty).param("description", "nothing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        Mockito.verifyNoInteractions(storage);
    }
}
