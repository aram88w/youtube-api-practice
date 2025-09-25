package youtube.youtube_api_practice;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.ReplyCommentDto;
import youtube.youtube_api_practice.dto.ReplyResponseDto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YoutubeApi {

    public static final String BASE = "https://www.googleapis.com/youtube/v3";

    private final WebClient webClient;
    private final String apiKey;

    public YoutubeApi(WebClient.Builder webClientBuilder, @Value("${youtube.api.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(BASE).build();
        this.apiKey = apiKey;
    }

    // 검색어로 유튜브 채널 ID 5개 가져오기
    public List<String> getChannelIdsBySearch(String keyword) {
        log.info("getChannelsIdBySearch {}", keyword);

        List<String> channelIds = new ArrayList<>();

        UriComponentsBuilder searchUri = UriComponentsBuilder.fromUriString(BASE + "/search")
                .queryParam("part", "snippet")
                .queryParam("q", keyword)
                .queryParam("type", "channel")
                .queryParam("maxResults", 5) // Fetch 3 results
                .queryParam("key", apiKey);

        JsonNode searchRoot = webClient.get()
                .uri(searchUri.build().encode().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (searchRoot == null || !searchRoot.has("items") || searchRoot.get("items").isEmpty()) {
            log.info("채널을 찾을 수 없습니다: {} ", keyword);
            throw new RuntimeException("채널을 찾을 수 없습니다: " + keyword);
        }

        for (JsonNode item : searchRoot.get("items")) {
            String channelId = item.path("id").path("channelId").asText();
//            String channelName = item.path("snippet").path("title").asText();
//            String channelDescription = item.path("snippet").path("description").asText();
//            String channelThumbnails= item.path("snippet").path("thumbnails").path("high").path("url").asText();

            channelIds.add(channelId);
        }

        return channelIds;
    }

    // 채널 ID로 Channel 객체 생성
    public Channel getChannelById(String channelId) {
        log.info("getChannelById {}", channelId);

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE + "/channels")
                .queryParam("part", "snippet,contentDetails,statistics")
                .queryParam("id", channelId)
                .queryParam("key", apiKey);

        JsonNode root = webClient.get()
                .uri(uri.build().encode().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null || !root.has("items") || root.get("items").isEmpty()) {
            log.info("채널 정보를 찾을 수 없습니다: {} ", channelId);
            throw new RuntimeException("채널 정보를 찾을 수 없습니다: " + channelId);
        }

        JsonNode item = root.get("items").get(0);
        String name = item.path("snippet").path("title").asText();
        String description = item.path("snippet").path("description").asText();
        String thumbnailUrl = item.path("snippet").path("thumbnails").path("high").path("url").asText();
        String uploadsPlaylistId = item.path("contentDetails").path("relatedPlaylists").path("uploads").asText();

        JsonNode subsNode = item.path("statistics").path("subscriberCount");
        Long subscriberCount = subsNode.isMissingNode() || subsNode.asText().isEmpty() ? 0L : Long.parseLong(subsNode.asText());

        log.info("description {}", description);

        return Channel.builder()
                .id(channelId)
                .uploadsPlaylistId(uploadsPlaylistId)
                .name(name)
                .description(description)
                .searchCount(0)
//                .lastSelectAt(LocalDateTime.now())
                .thumbnailUrl(thumbnailUrl)
                .subscriberCount(subscriberCount)
                .build();
    }


    // 채널에서 최근 비디오 id 50개 가져오기 (페이징)
    public void getVideosByChannel(Channel channel) {
        // 1. 채널의 공식 '업로드' 플레이리스트 ID를 사용합니다.
        String uploadsPlaylistId = channel.getUploadsPlaylistId();

        // 2. 기존 /search 대신 /playlistItems 엔드포인트를 사용합니다.
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE + "/playlistItems")
                .queryParam("part", "snippet") // snippet에 동영상 정보가 포함되어 있습니다.
                .queryParam("playlistId", uploadsPlaylistId)
                .queryParam("maxResults", 50)
                .queryParam("key", apiKey);

        JsonNode root = webClient.get()
                .uri(uri.build().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null || !root.has("items")) {
            // 사용자 예외로 추후 변경
            throw new RuntimeException("비디오가 없습니다");
        }

        for (JsonNode item : root.get("items")) {
            JsonNode snippet = item.path("snippet");

            // 3. playlistItems API의 응답 구조에 맞게 JSON 경로를 수정합니다.
            String videoId = snippet.path("resourceId").path("videoId").asText();
            String videoTitle = snippet.path("title").asText();
            // 썸네일 경로가 다를 수 있으므로, 가장 안전한 high 퀄리티 썸네일을 가져오도록 수정
            String videoThumbnail = snippet.path("thumbnails").path("high").path("url").asText();
            LocalDateTime videoPublishedAt = OffsetDateTime
                    .parse(snippet.path("publishedAt").asText())
                    .toLocalDateTime();

            Video video = Video.builder()
                    .id(videoId)
                    .title(videoTitle)
                    .channel(channel)
                    .thumbnailUrl(videoThumbnail)
                    .publishedAt(videoPublishedAt)
                    .build();

            channel.addVideo(video);
        }
    }

    //비디오의 최상위 댓글들 모두 가져오기 (페이징, maxResults 최대 100)
    public void getCommentsByVideo(Video video) {
        log.info("getCommentsByVideo {}", video);

        String videoId = video.getId();

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE + "/commentThreads")
                .queryParam("part", "snippet")
                .queryParam("videoId", videoId)
                .queryParam("maxResults", 10)            // 상위 10개
                .queryParam("order", "relevance")      // 좋아요/추천 위주
                .queryParam("key", apiKey);

        try {
            JsonNode root = webClient.get()
                    .uri(uri.build().toUri())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items")) {
                return; // 댓글이 없는 경우
            }

            for (JsonNode item : root.get("items")) {
                JsonNode snippet = item.path("snippet").path("topLevelComment").path("snippet");

                String commentId = item.path("snippet").path("topLevelComment").path("id").asText();
                String authorId = snippet.path("authorChannelId").path("value").asText(null); // null 허용
                String authorName = snippet.path("authorDisplayName").asText();
                String authorThumbnail = snippet.path("authorProfileImageUrl").asText(null); // null 허용
                String content = snippet.path("textDisplay").asText();
                int likeCount = snippet.path("likeCount").asInt(0);
                int replyCount = item.path("snippet").path("totalReplyCount").asInt(0);
                LocalDateTime publishedAt = OffsetDateTime
                        .parse(snippet.path("publishedAt").asText())
                        .toLocalDateTime();

                Comment comment = Comment.builder()
                        .id(commentId)
                        .authorId(authorId)
                        .authorName(authorName)
                        .authorThumbnailUrl(authorThumbnail)
                        .content(content)
                        .likeCount(likeCount)
                        .publishedAt(publishedAt)
                        .replyCount(replyCount)
                        .video(video)
                        .build();

                video.addComment(comment);
            }
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("댓글이 비활성화된 동영상입니다. videoId={}, 응답 본문: {}", videoId, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("댓글을 가져오는 중 오류가 발생했습니다. videoId={}", videoId, e);
        }
    }

    // 대댓글 가져오기
    public ReplyResponseDto getRepliesByComment(String commentId, String pageToken) {
        log.info("getRepliesByComment {} pageToken {}", commentId, pageToken);

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(BASE + "/comments")
                .queryParam("part", "snippet")
                .queryParam("parentId", commentId)
                .queryParam("maxResults", 10)
                .queryParam("key", apiKey);

        if (pageToken != null && !pageToken.isEmpty()) {
            uri.queryParam("pageToken", pageToken);
        }

        JsonNode root = webClient.get()
                .uri(uri.build().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null || !root.has("items")) {
            return new ReplyResponseDto(null); // 대댓글이 없는 경우
        }

        ReplyResponseDto replyResponseDto = new ReplyResponseDto(root.path("nextPageToken").asText(null));

        for (JsonNode item : root.get("items")) {
            JsonNode snippet = item.path("snippet");

            String authorName = snippet.path("authorDisplayName").asText();
            String authorThumbnail = snippet.path("authorProfileImageUrl").asText();
            String content = snippet.path("textDisplay").asText();
            int likeCount = snippet.path("likeCount").asInt(0);
            LocalDateTime publishedAt = OffsetDateTime
                    .parse(snippet.path("publishedAt").asText())
                    .toLocalDateTime();

            ReplyCommentDto replyCommentDto = ReplyCommentDto.builder()
                    .name(authorName)
                    .ThumbnailUrl(authorThumbnail)
                    .content(content)
                    .likeCount(likeCount)
                    .createdAt(publishedAt)
                    .build();

            replyResponseDto.getReplyComments().add(replyCommentDto);
        }

        return replyResponseDto;
    }
}