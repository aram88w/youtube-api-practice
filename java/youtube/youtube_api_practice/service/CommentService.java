package youtube.youtube_api_practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.SearchCache;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.repository.Video.VideoRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;
import youtube.youtube_api_practice.repository.Comment.CommentRepository;
import youtube.youtube_api_practice.repository.SearchCacheRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap; // 새로 추가
import java.util.concurrent.Executors;       // 새로 추가
import java.util.concurrent.ScheduledExecutorService; // 새로 추가
import java.util.concurrent.TimeUnit;        // 새로 추가
import jakarta.annotation.PostConstruct;      // 새로 추가
import jakarta.servlet.http.HttpServletRequest; // 새로 추가 (getComments 메서드 파라미터용)
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final CommentRepository commentRepository;
    private final SearchCacheRepository searchCacheRepository;
    private final VideoRepository videoRepository;

    // 조회수 중복 방지를 위한 캐시 (IP + ChannelId -> 마지막 조회 시간)
    private final Map<String, Long> viewCooldownCache = new ConcurrentHashMap<>();
    // 캐시 정리 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 5분마다 캐시 정리 (예시)
        scheduler.scheduleAtFixedRate(this::cleanupViewCooldownCache, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupViewCooldownCache() {
        long fiveMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        viewCooldownCache.entrySet().removeIf(entry -> entry.getValue() < fiveMinutesAgo);
        log.debug("View cooldown cache cleaned up. Current size: {}", viewCooldownCache.size());
    }


    @Transactional
    public Page<CommentResponseDto> getComments(String channelId, int page, int size, HttpServletRequest request) { // HttpServletRequest 추가
        log.info("getComments {} page={} size={}", channelId, page, size);

        String clientIp = request.getRemoteAddr(); // 클라이언트 IP 주소 가져오기

        // 1. 채널 조회
        Optional<Channel> channelOpt = channelRepository.findById(channelId);
        Channel channel;

        if (channelOpt.isEmpty() || channelOpt.get().getLastSelectAt() == null) {
            // 2a. 동기화가 필요한 경우 (채널이 없거나, 첫 댓글 조회)
            log.info("유튜브 API를 통해 최신 정보를 동기화합니다. channelId={}", channelId);

            // 기존 정보가 있으면 searchCount를 유지하고, 없으면 0에서 시작
            int currentSearchCount = channelOpt.map(Channel::getSearchCount).orElse(0);

            channel = youtubeApi.getChannelById(channelId);
            channel.setLastSelectAt(LocalDateTime.now());
            channel.setSearchCount(currentSearchCount); // 기존 searchCount 유지

            // 조회수 증가 로직 (쿨다운 적용)
            incrementSearchCountWithCooldown(channel, clientIp, channelId);

            channelRepository.upsertChannel(channel);

            List<Video> videos = youtubeApi.getVideosByChannel(channel, 50);
            videoRepository.upsertVideos(videos);

            for (Video video : videos) {
                List<Comment> comments = youtubeApi.getCommentsByVideo(video, 10);
                commentRepository.upsertComments(comments);
            }
        } else {
            // 2b. 동기화가 필요 없는 경우 (DB에만 의존)
            channel = channelOpt.get();

            // 조회수 증가 로직 (쿨다운 적용)
            incrementSearchCountWithCooldown(channel, clientIp, channelId);
            // @Transactional에 의해 메서드 종료 시 변경 감지(dirty checking)로 DB에 자동 반영됨
        }

        return findCommentsFromDb(channelId, page, size);
    }


    private void incrementSearchCountWithCooldown(Channel channel, String clientIp, String channelId) {
        String cacheKey = clientIp + ":" + channelId;
        long currentTime = System.currentTimeMillis();
        long cooldownPeriodMillis = TimeUnit.MINUTES.toMillis(5); // 5분 쿨다운

        if (!viewCooldownCache.containsKey(cacheKey) || (currentTime - viewCooldownCache.get(cacheKey) > cooldownPeriodMillis)) {
            channel.incrementSearchCount(); // 조회수 1 증가
            viewCooldownCache.put(cacheKey, currentTime); // 캐시 업데이트
            log.debug("Incremented search_count for channelId {} from IP {}", channelId, clientIp);
        } else {
            log.debug("Search_count not incremented for channelId {} from IP {} due to cooldown.", channelId, clientIp);
        }
    }

    private Page<CommentResponseDto> findCommentsFromDb(String channelId, int page, int size) {

        log.info("findCommentsFromDb {} page={} size={}", channelId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<Comment> comments = commentRepository.findByChannelOrderByLikeCountDesc(channelId, pageable);
        return comments.map(comment -> CommentResponseDto.builder().comment(comment).build());
    }


    public ReplyResponseDto getReplies(String commentId, String pageToken) {
        return youtubeApi.getRepliesByComment(commentId, pageToken);
    }

}

