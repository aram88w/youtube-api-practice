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

    // http://localhost:8080/admin/update/UCUj6rrhMTR9pipbAWBAMvUQ?videoLimit=300&commentLimit=30
    @GetMapping("/admin/update/{channelId}")
    public ResponseEntity<String> update(@PathVariable String channelId,
                                         @RequestParam(defaultValue = "300") int videoLimit,
                                         @RequestParam(defaultValue = "30") int commentLimit) {
        log.info("Update started: channelId={}, videoLimit={}, commentLimit={}", channelId, videoLimit, commentLimit);

        long start = System.currentTimeMillis();

        commentService.update(channelId, videoLimit, commentLimit);

        long end = System.currentTimeMillis();
        long elapsed = end - start; // 밀리초

        double seconds = elapsed / 1000.0; // 초 단위로 변환
        log.info("걸린 시간: {}초", seconds);

        return ResponseEntity.ok("Update started for channelId=" + channelId);
    }
}
