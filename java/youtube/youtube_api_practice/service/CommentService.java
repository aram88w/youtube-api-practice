package youtube.youtube_api_practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.SearchCache;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.repository.ChannelRepository;
import youtube.youtube_api_practice.repository.CommentRepository;
import youtube.youtube_api_practice.repository.SearchCacheRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final CommentRepository commentRepository;
    private final SearchCacheRepository searchCacheRepository;

    @Transactional
    public List<ChannelResponseDto> getChannelIds(String search) {
        log.info("getChannels {}", search);

        Optional<SearchCache> cacheOpt = searchCacheRepository.findById(search);

        // 검색한 적이 있다면
        if (cacheOpt.isPresent()) {
            SearchCache cache = cacheOpt.get();

            // 갱신한지 24시간이 지나지 않았으면 DB에서 바로 조회
            if (cache.getLastSearchedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                Set<String> channelIds = cache.getChannelIds();
                List<Channel> chennels = channelRepository.findByIdInOrderBySubscriberCountDesc(channelIds);

                return ChannelResponseDto.ChannelToDto(chennels);

            } else { // 갱신한지 24시간이 지났으면 API 요청 후 갱신
                List<String> channelIdsBySearch = youtubeApi.getChannelIdsBySearch(search);
                Set<String> channelIds = cache.getChannelIds();

                channelIds.addAll(channelIdsBySearch);
                cache.setChannelIds(channelIds);
                cache.setLastSearchedAt(LocalDateTime.now());

                searchCacheRepository.save(cache);

                return saveChannels(channelIds);
            }
        } else { // 처음 검색하면
            List<String> channelIdsBySearch = youtubeApi.getChannelIdsBySearch(search);
            Set<String> channelIds = new HashSet<>(channelIdsBySearch);

            SearchCache cache = new SearchCache();
            cache.setKeyword(search);
            cache.setLastSearchedAt(LocalDateTime.now());
            cache.setChannelIds(channelIds);

            searchCacheRepository.save(cache);

            return saveChannels(channelIds);
        }
    }

    public List<ChannelResponseDto> saveChannels(Set<String> channelIds) {
        log.info("saveChannels {}", channelIds);

        List<Channel> result = new ArrayList<>();

        for (String channelId : channelIds) {
            Optional<Channel> channelOpt = channelRepository.findById(channelId);

            Channel channel = youtubeApi.getChannelById(channelId);
            result.add(channel);

            // 갱신
            if (channelOpt.isPresent()) {
                channelOpt.get().updateChannelInfo(channel.getName(), channel.getDescription(), channel.getThumbnailUrl(),
                        channel.getUploadsPlaylistId(), channelOpt.get().getLastSelectAt(), channel.getSubscriberCount());
                channelRepository.save(channelOpt.get());
            } else { // 새로 저장
                channelRepository.save(channel);
            }
        }

        return ChannelResponseDto.sortBySubscriberCountDesc(ChannelResponseDto.ChannelToDto(result));
    }


    @Transactional
    public List<CommentResponseDto> getComments(String channelId) {
        log.info("getComments {}", channelId);

        Optional<Channel> channelOpt = channelRepository.findById(channelId);

        //채널이 존재하고, 마지막 검색시간이 null이 아니고 1시간 이내일 경우 DB에서 조회 후 리턴
        if (channelOpt.isPresent()
                && channelOpt.get().getLastSelectAt() != null
                && channelOpt.get().getLastSelectAt().isAfter(LocalDateTime.now().minusHours(1))) {
            log.info("DB에서 댓글을 조회합니다. channelId={}", channelId);
            return findCommentsFromDb(channelId);
        }

        log.info("유튜브 API를 통해 최신 정보를 동기화합니다. channelId={}", channelId);

        Channel channel = channelOpt.orElse(null);

        channel.setLastSelectAt(LocalDateTime.now());
        //기존 채널: 비디오/댓글 정보를 지우고 DB에 즉시 반영 (flush)
        channel.getVideos().clear();
        channelRepository.saveAndFlush(channel);

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

