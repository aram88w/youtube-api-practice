package youtube.youtube_api_practice.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.client.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.repository.Comment.CommentRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 동시성 제어 및 요청 분배를 담당하는 오케스트레이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final CommentRepository commentRepository;
    private final CommentSyncService commentSyncService; // 동기화 작업용 서비스

    // 가벼운 초기 동기화 작업 목록
    private final Map<String, CompletableFuture<Void>> initialSyncInProgress = new ConcurrentHashMap<>();
    // 무거운 추가 동기화 작업 목록
    private final Map<String, CompletableFuture<Void>> moreCommentsInProgress = new ConcurrentHashMap<>();

    // 조회수 중복 방지용 캐시
    private final Map<String, Long> viewCooldownCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::cleanupViewCooldownCache, 5, 5, TimeUnit.MINUTES);
    }

    private void cleanupViewCooldownCache() {
        long fiveMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        viewCooldownCache.entrySet().removeIf(entry -> entry.getValue() < fiveMinutesAgo);
        log.debug("조회수 쿨다운 캐시 정리 완료. 현재 크기: {}", viewCooldownCache.size());
    }

    @Transactional // 동기화가 필요 없는 경우의 조회수 변경 감지를 위해 트랜잭션 유지
    public CompletableFuture<Page<CommentResponseDto>> getComments(String channelId, int page, int size, HttpServletRequest request) {
        log.info("getComments 요청 받음: {}, 페이지: {}, 크기: {}", channelId, page, size);

        incrementSearchCountWithCooldown(channelId, request.getRemoteAddr());

        Optional<Channel> channelOpt = channelRepository.findById(channelId);

        CompletableFuture<Void> syncFuture = CompletableFuture.completedFuture(null);

        if (channelOpt.isEmpty() || channelOpt.get().getLastSelectedAt() == null) {
            // 초기 동기화가 필요한 경우, 동시성 제어 로직 수행
            syncFuture = initialSyncInProgress.computeIfAbsent(channelId, id -> {
                log.info("초기 동기화 필요. 작업 시작: {}", id);
                return CompletableFuture.runAsync(() -> commentSyncService.getCommentSync(id))
                        .whenComplete((v, e) -> {
                            log.info("초기 동기화 작업 완료. 맵에서 제거: {}", id);
                            initialSyncInProgress.remove(id);
                        });
            });
        }

        // 동기화 작업(필요했다면)이 끝난 후, DB에서 데이터를 조회하여 반환
        return syncFuture.thenCompose(v ->
                CompletableFuture.completedFuture(findCommentsFromDb(channelId, page, size))
        );
    }

    public CompletableFuture<Page<CommentResponseDto>> getMoreComments(String channelId) {
        log.info("getMoreComments 요청 받음: {}", channelId);

        // 추가 동기화가 필요한 경우, 동시성 제어 로직 수행
        CompletableFuture<Void> syncFuture = moreCommentsInProgress.computeIfAbsent(channelId, id -> {
            log.info("추가 동기화 작업 시작: {}", id);
            return CompletableFuture.runAsync(() -> commentSyncService.getMoreCommentSync(id))
                    .whenComplete((v, e) -> {
                        log.info("추가 동기화 작업 완료. 맵에서 제거: {}", id);
                        moreCommentsInProgress.remove(id);
                    });
        });

        // 동기화 작업이 끝난 후, DB에서 데이터를 조회하여 반환
        return syncFuture.thenCompose(v ->
                CompletableFuture.completedFuture(findCommentsFromDb(channelId, 0, 10))
        );
    }

    public Page<CommentResponseDto> findCommentsFromDb(String channelId, int page, int size) {
        log.info("DB에서 댓글 조회: {}, 페이지: {}, 크기: {}", channelId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByChannelOrderByLikeCountDesc(channelId, pageable);
        return comments.map(comment -> CommentResponseDto.builder().comment(comment).build());
    }

    public void incrementSearchCountWithCooldown(String channelId, String clientIp) {
        String cacheKey = clientIp + ":" + channelId;
        long currentTime = System.currentTimeMillis();
        long cooldownPeriodMillis = TimeUnit.MINUTES.toMillis(5);

        Optional<Channel> channel = channelRepository.findById(channelId);

        if (channel.isEmpty()) { return; }

        if (!viewCooldownCache.containsKey(cacheKey) || (currentTime - viewCooldownCache.get(cacheKey) > cooldownPeriodMillis)) {
            channel.get().incrementSearchCount();
            viewCooldownCache.put(cacheKey, currentTime);
            log.debug("조회수 증가: 채널 ID {} from IP {}", channelId, clientIp);
        } else {
            log.debug("쿨다운으로 인해 조회수 미증가: 채널 ID {} from IP {}", channelId, clientIp);
        }
    }

    @Cacheable("replies")
    public ReplyResponseDto getReplies(String commentId, String pageToken) {
        log.info("댓글의 답글 조회: {}, 페이지 토큰: {}", commentId, pageToken);
        return youtubeApi.getRepliesByComment(commentId, pageToken);
    }
}