package youtube.youtube_api_practice.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.CommentStatus;
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


    private final WebClient webClient;
    private final String apiKey;

    public YoutubeApi(WebClient youtubeWebClient, @Value("${youtube.api.key}") String apiKey) {
        this.webClient = youtubeWebClient;
        this.apiKey = apiKey;
    }

    // 검색어로 유튜브 채널 ID 10개 가져오기
    public List<String> getChannelIdsBySearch(String keyword) {
        log.info("getChannelsIdBySearch {}", keyword);

        List<String> channelIds = new ArrayList<>();

        JsonNode searchRoot = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", keyword)
                        .queryParam("type", "channel")
                        .queryParam("maxResults", 10)
                        .queryParam("key", apiKey)
                        .build())
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

    // 검색어로 유튜브 채널 ID 10개 가져오기 (비동기)
    public Mono<List<String>> getChannelIdsBySearchAsync(String keyword) {
        log.info("getChannelsIdBySearchAsync {}", keyword);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", keyword)
                        .queryParam("type", "channel")
                        .queryParam("maxResults", 10)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(searchRoot -> {
                    if (searchRoot == null || !searchRoot.has("items") || searchRoot.get("items").isEmpty()) {
                        log.info("채널을 찾을 수 없습니다: {}", keyword);
                        return Mono.error(new RuntimeException("채널을 찾을 수 없습니다: " + keyword));
                    }

                    List<String> channelIds = new ArrayList<>();
                    for (JsonNode item : searchRoot.get("items")) {
                        String channelId = item.path("id").path("channelId").asText();
                        channelIds.add(channelId);
                    }
                    return Mono.just(channelIds);
                });
    }


    // 채널 ID로 Channel 객체 생성
    public Channel getChannelById(String channelId) {
        log.info("getChannelById {}", channelId);

        JsonNode root = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/channels")
                        .queryParam("part", "snippet,contentDetails,statistics")
                        .queryParam("id", channelId)
                        .queryParam("key", apiKey)
                        .build())
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

        return Channel.builder()
                .id(channelId)
                .uploadsPlaylistId(uploadsPlaylistId)
                .name(name)
                .description(description)
                .searchCount(0)
//                .lastSelectAt(LocalDateTime.now())
                .thumbnailUrl(thumbnailUrl)
                .subscriberCount(subscriberCount)
                .commentStatus(CommentStatus.COMMENT_NONE)
                .build();
    }

    // 채널에서 최근 비디오 limit만큼 가져오기
    public List<Video> getVideosByChannel(Channel channel, int limit) {
        log.info("getVideosByChannel {}", channel);
        List<Video> videos = new ArrayList<>();

        String uploadsPlaylistId = channel.getUploadsPlaylistId();
        String nextPageToken = null;
        int remaining = limit;

        do {
            int maxResults = Math.min(remaining, 50); // 한 번에 가져올 수 있는 최대 50
            final String currentPageToken = nextPageToken;

            JsonNode root = webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/playlistItems")
                                .queryParam("part", "snippet")
                                .queryParam("playlistId", uploadsPlaylistId)
                                .queryParam("maxResults", maxResults)
                                .queryParam("key", apiKey);

                        if (currentPageToken != null) {
                            uriBuilder.queryParam("pageToken", currentPageToken);
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items")) {
                break;
            }

            for (JsonNode item : root.get("items")) {
                JsonNode snippet = item.path("snippet");
                String videoId = snippet.path("resourceId").path("videoId").asText();
                String videoTitle = snippet.path("title").asText();
                String videoThumbnail = snippet.path("thumbnails").path("high").path("url").asText();
                LocalDateTime videoPublishedAt = OffsetDateTime
                        .parse(snippet.path("publishedAt").asText())
                        .toLocalDateTime();

                videos.add(Video.builder()
                        .id(videoId)
                        .title(videoTitle)
                        .channel(channel)
                        .thumbnailUrl(videoThumbnail)
                        .publishedAt(videoPublishedAt)
                        .build()
                );
            }

            remaining -= root.get("items").size();
            nextPageToken = root.has("nextPageToken") ? root.path("nextPageToken").asText() : null;

        } while (remaining > 0 && nextPageToken != null);

        return videos;
    }



    //비디오의 최상위 댓글들 모두 가져오기 (페이징, maxResults 최대 100)
    public List<Comment> getCommentsByVideo(Video video, int limit) {
        log.info("getCommentsByVideo {}", video);
        List<Comment> comments = new ArrayList<>();

        String videoId = video.getId();

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/commentThreads")
                            .queryParam("part", "snippet")
                            .queryParam("videoId", videoId)
                            .queryParam("maxResults", limit)
                            .queryParam("order", "relevance")      // 좋아요/추천 위주
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items")) {
                return comments; // 댓글이 없는 경우
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

                comments.add(comment);
            }
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("댓글이 비활성화된 동영상입니다. videoId={}, 응답 본문: {}", videoId, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("댓글을 가져오는 중 오류가 발생했습니다. videoId={}", videoId, e);
        }
        return comments;
    }

    // 대댓글 가져오기
    public ReplyResponseDto getRepliesByComment(String commentId, String pageToken) {
        log.info("getRepliesByComment {} pageToken {}", commentId, pageToken);

        JsonNode root = webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/comments")
                            .queryParam("part", "snippet")
                            .queryParam("parentId", commentId)
                            .queryParam("maxResults", 10)
                            .queryParam("key", apiKey);

                    if (pageToken != null) {
                        uriBuilder.queryParam("pageToken", pageToken);
                    }

                    return uriBuilder.build();
                })
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