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

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final CommentRepository commentRepository;
    private final SearchCacheRepository searchCacheRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public List<ChannelResponseDto> getChannelIds(String search) {
        log.info("getChannelIds {}", search);

        // 1. DB에서 검색어 캐시 조회
        Optional<SearchCache> cacheOpt = searchCacheRepository.findById(search);
        SearchCache cache;

        if (cacheOpt.isEmpty()) {
            // 2-1. 처음 검색하면 유튜브 API로 가져와서 저장
            Set<String> channelIds = new HashSet<>(youtubeApi.getChannelIdsBySearch(search));
            cache = new SearchCache();
            cache.setKeyword(search);
            cache.setLastSearchedAt(LocalDateTime.now());
            cache.setChannelIds(channelIds);

            searchCacheRepository.save(cache);
            saveChannels(channelIds);

        } else {
            cache = cacheOpt.get();

            // 2-2. 마지막 갱신이 오래되었으면 API로 갱신
            if (cache.getLastSearchedAt().isBefore(LocalDateTime.now().minusHours(100))) {
                Set<String> channelIds = cache.getChannelIds();
                channelIds.addAll(youtubeApi.getChannelIdsBySearch(search));
                cache.setChannelIds(channelIds);
                cache.setLastSearchedAt(LocalDateTime.now());

                searchCacheRepository.save(cache);
                saveChannels(channelIds);
            }
        }

        // 3. 캐시에서 채널 ID 가져와 DB 조회
        List<Channel> channels = channelRepository.findByIdInOrderBySubscriberCountDesc(cache.getChannelIds());
        return ChannelResponseDto.ChannelToDto(channels);
    }

    public void saveChannels(Set<String> channelIds) {
        log.info("saveChannels {}", channelIds);

        for (String channelId : channelIds) {
            Channel channel = youtubeApi.getChannelById(channelId);
            channelRepository.upsertChannel(channel);
        }
    }



//    @Transactional
//    public Page<CommentResponseDto> getComments(String channelId, int page, int size) {
//        log.info("getComments {} page={} size={}", channelId, page, size);
//
//        Optional<Channel> channelOpt = channelRepository.findById(channelId);
//
//        // 채널이 존재하고, 마지막 검색시간이 null이 아니고 1시간 이내일 경우 DB에서 조회 후 리턴
//        // 일단 그냥 채널이 존재하면 DB에서 가져옴
//        if (channelOpt.isPresent()) {
////                && channelOpt.get().getLastSelectAt() != null
////                && channelOpt.get().getLastSelectAt().isAfter(LocalDateTime.now().minusHours(1))) {
//            log.info("DB에서 댓글을 조회합니다. channelId={}", channelId);
//            return findCommentsFromDb(channelId, page, size);
//        }
//
//        log.info("유튜브 API를 통해 최신 정보를 동기화합니다. channelId={}", channelId);
//
//        Channel channel = channelOpt.orElseGet(() -> youtubeApi.getChannelById(channelId));
//
//        channelRepository.upsertChannel(channel);
//
//        //API를 통해 새로운 정보 가져오기
//        List<Video> videos = youtubeApi.getVideosByChannel(channel, 50);
//        videoRepository.upsertVideos(videos);
//        for (Video video : videos) {
//            List<Comment> comments = youtubeApi.getCommentsByVideo(video, 10);
//            commentRepository.upsertComments(comments);
//        }
//
//        return findCommentsFromDb(channelId, page, size);
//    }

    @Transactional
    public Page<CommentResponseDto> getComments(String channelId, int page, int size) {
        log.info("getComments {} page={} size={}", channelId, page, size);

        Optional<Channel> channelOpt = channelRepository.findById(channelId);

        // 댓글을 조회한 적 없으면
        if (channelOpt.isEmpty() || channelOpt.get().getLastSelectAt() == null) {
            log.info("유튜브 API를 통해 최신 정보를 동기화합니다. channelId={}", channelId);
            log.info("channelOpt.isEmpty() = {}", channelOpt.isEmpty());
            log.info("channelOpt.get().getLastSelectAt() = {}", channelOpt.get().getLastSelectAt());

            Channel channel = youtubeApi.getChannelById(channelId);
            channel.setLastSelectAt(LocalDateTime.now());
            channelRepository.upsertChannel(channel);

            List<Video> videos = youtubeApi.getVideosByChannel(channel, 50);
            videoRepository.upsertVideos(videos);

            for (Video video : videos) {
                List<Comment> comments = youtubeApi.getCommentsByVideo(video, 10);
                commentRepository.upsertComments(comments);
            }
        }

        return findCommentsFromDb(channelId, page, size);
    }

    private Page<CommentResponseDto> findCommentsFromDb(String channelId, int page, int size) {

        log.info("findCommentsFromDb {} page={} size={}", channelId, page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<Comment> comments = commentRepository.findByChannelOrderByLikeCountDesc(channelId, pageable);
        return comments.map(comment -> CommentResponseDto.builder().comment(comment).build());
    }

    @Transactional(readOnly = true)
    public ChannelResponseDto getChannelDetails(String channelId) {
        return channelRepository.findById(channelId)
                .map(channel -> ChannelResponseDto.builder()
                        .id(channel.getId())
                        .name(channel.getName())
                        .description(channel.getDescription())
                        .thumbnailUrl(channel.getThumbnailUrl())
                        .subscriberCount(channel.getSubscriberCount())
                        .build())
                .orElseThrow(() -> new RuntimeException("Channel not found: " + channelId));
    }

    public ReplyResponseDto getReplies(String commentId, String pageToken) {
        return youtubeApi.getRepliesByComment(commentId, pageToken);
    }
}

