package com.leandre.photouploader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PhotoController {

    private final PhotoRepository photos;
    private final StorageService storage;
    private final String version;
    private final String commit;
    private final String builtAt;

    public PhotoController(
            PhotoRepository photos,
            StorageService storage,
            @Value("${app.version:local}") String version,
            @Value("${app.commit:unknown}") String commit,
            @Value("${app.builtAt:unknown}") String builtAt) {
        this.photos = photos;
        this.storage = storage;
        this.version = version;
        this.commit = commit;
        this.builtAt = builtAt;
    }

    @GetMapping("/")
    public String gallery(Model model) {
        List<PhotoView> view = photos.findAllByOrderByCreatedAtDesc().stream()
                .map(photo -> new PhotoView(storage.urlFor(photo.getObjectKey()), photo.getDescription()))
                .toList();

        model.addAttribute("photos", view);
        model.addAttribute("version", version);
        return "index";
    }

    @PostMapping("/upload")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            RedirectAttributes redirect) throws IOException {

        if (file.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose an image to upload.");
            return "redirect:/";
        }

        String key = storage.store(file);
        photos.save(new Photo(key, description));
        return "redirect:/";
    }

    /** Target of the ALB health check - must stay cheap and must not touch S3. */
    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "ok", "commit", commit);
    }

    @GetMapping("/api/version")
    @ResponseBody
    public Map<String, String> version() {
        return Map.of("version", version, "commit", commit, "builtAt", builtAt);
    }

    public record PhotoView(String url, String description) {
    }
}
