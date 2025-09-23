package youtube.youtube_api_practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.repository.ChannelRepository;
import youtube.youtube_api_practice.repository.CommentRepository;
import youtube.youtube_api_practice.repository.VideoRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    // 검색어로 채널 Id 가져오기
    public String getChannelId(String search) {
        log.info("getChannelId {}", search);
        return youtubeApi.getChannelIdBySearch(search);
    }

    @Transactional
    public List<CommentResponseDto> getComments(String channelId) {

        Optional<Channel> channelOpt = channelRepository.findById(channelId);

        //채널이 존재하고, 마지막 검색시간이 1시간 이내일 경우 DB에서 조회 후 리턴
        if(channelOpt.isPresent() && channelOpt.get().getLastSearchedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            log.info("DB에서 댓글을 조회합니다. channelId={}", channelId);
            return findCommentsFromDb(channelId);
        }

        log.info("유튜브 API를 통해 최신 정보를 동기화합니다. channelId={}", channelId);

        Channel channel = channelOpt.orElse(null);

        if (channel != null) {
            //채널 정보 업데이트 (검색 시간 갱신)
            channel.updateChannelInfo(channel.getName(), channel.getDescription(), channel.getThumbnailUrl(), LocalDateTime.now());

            //기존 채널: 비디오/댓글 정보를 지우고 DB에 즉시 반영 (flush)
            channel.getVideos().clear();
            channelRepository.saveAndFlush(channel);
        } else {
            //새로운 채널
            channel = youtubeApi.getChannelById(channelId);
        }

        //API를 통해 새로운 정보 가져오기
        youtubeApi.getVideosByChannel(channel);
        for (Video video : channel.getVideos()) {
            youtubeApi.getCommentsByVideo(video);
        }

        channelRepository.save(channel);

        return findCommentsFromDb(channelId);
    }

    private List<CommentResponseDto> findCommentsFromDb(String channelId) {

        log.info("findCommentsFromDb {}", channelId);

        return commentRepository.findByChannelOrderByLikeCount(channelId)
                .stream()
                .map(comment -> CommentResponseDto.builder().comment(comment).build())
                .collect(Collectors.toList());
    }
}

