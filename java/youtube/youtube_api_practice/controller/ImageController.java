package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import youtube.youtube_api_practice.client.ImageClient; // Import ImageClient

import java.util.Optional; // Import Optional
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/youtube/api")
@RequiredArgsConstructor
public class ImageController {

    private final ImageClient imageClient; // Inject ImageClient

    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage(@RequestParam("url") String url) {
        Optional<byte[]> imageBytesOptional = imageClient.fetchImage(url);

        if (imageBytesOptional.isPresent()) {
            byte[] imageBytes = imageBytesOptional.get();

            // Set cache control headers for 24 hours
            CacheControl cacheControl = CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic();

            // Return image bytes with correct headers
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // A default, can be improved
                    .cacheControl(cacheControl)
                    .body(imageBytes);
        } else {
            // ImageClient already logged the error, just return appropriate status
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
