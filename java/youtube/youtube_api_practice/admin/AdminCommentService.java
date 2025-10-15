package youtube.youtube_api_practice.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import youtube.youtube_api_practice.client.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.CommentStatus;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.provider.YoutubeProvider;
import youtube.youtube_api_practice.repository.Comment.CommentRepository;
import youtube.youtube_api_practice.repository.Video.VideoRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final YoutubeApi youtubeApi;
    private final YoutubeProvider youtubeProvider;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

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

}

/**
 * 채널 1 + 비디오 100 + 댓글 30 = 131
 * 채널 10개 = 1300
 * 채널 70개 = 8000
 */
