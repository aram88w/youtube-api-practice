package youtube.youtube_api_practice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.client.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.CommentStatus;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.repository.Comment.CommentRepository;
import youtube.youtube_api_practice.repository.Video.VideoRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실제 동기화 작업과 트랜잭션을 담당하는 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentSyncService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    /**
     * 가벼운 초기 동기화 작업
     */
    @Transactional
    public void getCommentSync(String channelId) {
        log.info("performInitialSync 시작: {}", channelId);
        long start = System.currentTimeMillis();

        // API를 통해 최신 채널 정보를 가져옴
        Channel channel = youtubeApi.getChannelById(channelId);

        // 가벼운 동기화: 최근 영상 33개
        List<Video> videos = youtubeApi.getVideosByChannel(channel, 33);
        videoRepository.upsertVideos(videos);

        for (Video video : videos) {
            // 영상당 댓글 30개
            List<Comment> comments = youtubeApi.getCommentsByVideo(video, 30);
            commentRepository.upsertComments(comments);
        }

        // 모든 작업이 끝난 후 상태 업데이트
        channel.setLastSelectAt(LocalDateTime.now());
        channel.setCommentStatus(CommentStatus.COMMENT_BASIC);
        channelRepository.upsertChannel(channel);

        long end = System.currentTimeMillis();
        log.info("performInitialSync 걸린 시간: {}초", (end - start) / 1000.0);
    }

    /**
     * 무거운 추가 동기화 작업
     */
    @Transactional
    public void getMoreCommentSync(String channelId) {
        log.info("performMoreCommentsSync 시작: {}", channelId);
        long start = System.currentTimeMillis();

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found with id: " + channelId));

        // 무거운 동기화: 최근 영상 100개
        List<Video> videos = youtubeApi.getVideosByChannel(channel, 100);
        videoRepository.upsertVideos(videos);

        for (Video video : videos) {
            // 영상당 댓글 30개
            List<Comment> comments = youtubeApi.getCommentsByVideo(video, 30);
            commentRepository.upsertComments(comments);
        }

        // 모든 작업이 끝난 후 상태 업데이트
        channel.setCommentStatus(CommentStatus.COMMENT_EXTENDED);
        channelRepository.upsertChannel(channel);

        long end = System.currentTimeMillis();
        log.info("performMoreCommentsSync 걸린 시간: {}초", (end - start) / 1000.0);
    }
}