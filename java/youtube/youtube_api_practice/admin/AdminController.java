package youtube.youtube_api_practice.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminCommentService commentService;

    @GetMapping("/")
    public String home() {
        return "Hello World!";
    }

    // http://localhost:8080/admin/update/UC2jkfOUD5KNbIpkb77-chqQ?videoLimit=100&commentLimit=30
    @GetMapping("/admin/update/{channelId}")
    public ResponseEntity<String> update(@PathVariable String channelId,
                                         @RequestParam(defaultValue = "100") int videoLimit,
                                         @RequestParam(defaultValue = "30") int commentLimit) {

        commentService.update(channelId, videoLimit, commentLimit);

        return ResponseEntity.ok("Update started for channelId=" + channelId);
    }

    @GetMapping("/admin/allupdate")
    public ResponseEntity<String> allUpdate(@RequestParam int limit) {
        commentService.allUpdate(limit);

        return ResponseEntity.ok("Update started for allUpdate");
    }
}
