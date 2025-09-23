package youtube.youtube_api_practice.home;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

//    @GetMapping("/video/{name}")
//    public JsonNode getVideo(@PathVariable String name) {
//        return getCommentsByVideo(name);
//    }
//
//    public static final String BASE = "https://www.googleapis.com/youtube/v3";
//
//    private final WebClient webClient;
//    private final String apiKey;
//
//    public HomeController(WebClient.Builder webClientBuilder, @Value("${youtube.api.key}") String apiKey) {
//        this.webClient = webClientBuilder.baseUrl(BASE).build();
//        this.apiKey = apiKey;
//    }
//
//    public JsonNode getCommentsByVideo(String videoId) {
//
//        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE + "/commentThreads")
//                .queryParam("part", "snippet")
//                .queryParam("videoId", videoId)
//                .queryParam("maxResults", 20)            // 상위 20개
//                .queryParam("order", "relevance")      // 좋아요/추천 위주
//                .queryParam("key", apiKey);
//
//        try {
//            JsonNode root = webClient.get()
//                    .uri(uri.build().toUri())
//                    .retrieve()
//                    .bodyToMono(JsonNode.class)
//                    .block();
//
//            return root;
//
//        } catch (WebClientResponseException.Forbidden e) {
//            log.warn("댓글이 비활성화된 동영상입니다. videoId={}, 응답 본문: {}", videoId, e.getResponseBodyAsString());
//            return null;
//        } catch (Exception e) {
//            log.error("댓글을 가져오는 중 오류가 발생했습니다. videoId={}", videoId, e);
//            return null;
//        }
//    };


}
