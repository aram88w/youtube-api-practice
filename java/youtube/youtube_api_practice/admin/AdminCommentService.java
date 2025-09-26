package youtube.youtube_api_practice.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;
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
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void update(String ChannelId, int videoLimit, int commentLimit) {
        Channel newChannel = youtubeApi.getChannelById(ChannelId);
        newChannel.setLastSelectAt(LocalDateTime.now());
        channelRepository.upsertChannel(newChannel);

        List<Video> videos = youtubeApi.getVideosByChannel(newChannel, videoLimit);
        videoRepository.upsertVideos(videos);

        for (Video video : videos) {
            List<Comment> comments = youtubeApi.getCommentsByVideo(video, commentLimit);
            commentRepository.upsertComments(comments);
        }
    }

}
