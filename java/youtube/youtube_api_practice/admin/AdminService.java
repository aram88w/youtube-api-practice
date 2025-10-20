package youtube.youtube_api_practice.admin;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.CommentStatus;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.client.YoutubeProvider;
import youtube.youtube_api_practice.repository.Comment.CommentRepository;
import youtube.youtube_api_practice.repository.Video.VideoRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final YoutubeProvider youtubeProvider;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final AdminRepository adminRepository;
    private final WebClient webClient;
    @Value("${youtube.api.key}")
    private String apiKey;

    @Transactional
    public void update(String channelId, int videoLimit, int commentLimit) {
        log.info("update started channelId={}, videoLimit={}, commentLimit={}", channelId, videoLimit, commentLimit);

        long start = System.currentTimeMillis();

        Channel newChannel = youtubeProvider.fetchChannel(channelId);
        newChannel.setCommentStatus(CommentStatus.COMMENT_EXTENDED);
        newChannel.setLastSelectAt(LocalDateTime.now());
        channelRepository.upsertChannel(newChannel);

        List<Video> videos = youtubeProvider.fetchVideos(newChannel, videoLimit);
        videoRepository.upsertVideos(videos);

        for (Video video : videos) {
            log.info("videoId={} videoTitle={}", video.getId(), video.getTitle());
            List<Comment> comments = youtubeProvider.fetchComments(video, commentLimit);
            for (Comment comment : comments) {
                log.info("comment {}", comment);
            }
            commentRepository.upsertComments(comments);
        }

        long end = System.currentTimeMillis();
        long elapsed = end - start; // 밀리초

        double seconds = elapsed / 1000.0; // 초 단위로 변환
        log.info("걸린 시간: {}초", seconds);
    }


    public void allUpdate(int limit) {
        log.info("allUpdate started");

        long start = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(0, limit);

        List<Channel> channels = channelRepository.findTopChannels(10000L, pageable);
        for (Channel channel : channels) {
            update(channel.getId(), 100, 30);
        }

        long end = System.currentTimeMillis();
        long elapsed = end - start; // 밀리초

        double seconds = elapsed / 1000.0; // 초 단위로 변환
        log.info("걸린 시간: {}초", seconds);
    }

    /**
     *  이거 comment_status를 다 BASIC으로 바꾸고 BASIC을 다 업데이트 하자
     */
    public void allVideoThumbnailUpdate() {
        log.info("videoThumbnailUpdate started");
        // commentStatus가 EXTEND인 채널 리스트로 가져오기
        List<Channel> channels = adminRepository.getChannelsExtended();
        // 채널의 비디오를 전부 가져와서 api 요청 후 upsert
        for (Channel channel : channels) {
            log.info("channel name {}", channel.getName());
            List<Video> videos = new ArrayList<>();

            channel.getVideos().forEach(video -> {
                videos.add(getVideo(video.getId(), channel));
            });
            videoRepository.upsertVideos(videos);

            channel.setCommentStatus(CommentStatus.COMMENT_EXTENDED);
            channelRepository.upsertChannel(channel);
        }


    }

    public Video getVideo(String videoId, Channel channel) {
        log.info("getVideo {} started", videoId);

        try {
            JsonNode root = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("part", "snippet")
                            .queryParam("id", videoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items") || root.get("items").isEmpty()) {
                log.warn("Youtube API로부터 비정상적인 비디오 목록 응답을 받았습니다. {}", videoId);
                return null;
            }

            JsonNode snippet = root.get("items").get(0).path("snippet");
            String videoTitle = snippet.path("title").asText();
            // --- 썸네일 폴백 로직 시작 ---
            JsonNode thumbnailsNode = snippet.path("thumbnails");
            String videoThumbnail;
            if (thumbnailsNode.has("maxres")) {
                videoThumbnail = thumbnailsNode.path("maxres").path("url").asText();
            } else if (thumbnailsNode.has("standard")) {
                videoThumbnail = thumbnailsNode.path("standard").path("url").asText();
            } else if (thumbnailsNode.has("high")) {
                videoThumbnail = thumbnailsNode.path("high").path("url").asText();
            } else if (thumbnailsNode.has("medium")) {
                videoThumbnail = thumbnailsNode.path("medium").path("url").asText();
            } else {
                videoThumbnail = thumbnailsNode.path("default").path("url").asText();
            }
            // --- 썸네일 폴백 로직 끝 ---
            LocalDateTime videoPublishedAt = OffsetDateTime
                    .parse(snippet.path("publishedAt").asText())
                    .toLocalDateTime();

            return Video.builder()
                    .id(videoId)
                    .title(videoTitle)
                    .channel(channel)
                    .thumbnailUrl(videoThumbnail)
                    .publishedAt(videoPublishedAt)
                    .build();
        } catch (Exception e) {
            log.error("getVideo {} exception", videoId, e);
            return null;
        }
    }

}

/**
 * 채널 1 + 비디오 100 + 댓글 30 = 131
 * 채널 10개 = 1300
 * 채널 70개 = 8000
 */
