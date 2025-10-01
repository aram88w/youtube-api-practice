package youtube.youtube_api_practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.SearchCache;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.repository.SearchCacheRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeApi youtubeApi;
    private final ChannelRepository channelRepository;
    private final SearchCacheRepository searchCacheRepository;


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

        // 1. 기존 채널 정보를 Map으로 한번에 조회
        Map<String, Channel> existingChannelMap = channelRepository.findAllById(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));

        // 2. API 호출 및 upsert 진행
        for (String channelId : channelIds) {
            Channel newChannel = youtubeApi.getChannelById(channelId);
            Channel existingChannel = existingChannelMap.get(channelId);

            // 3. 기존 채널 정보가 있으면 lastSelectAt과 searchCount 값을 유지
            if (existingChannel != null) {
                newChannel.setLastSelectAt(existingChannel.getLastSelectAt());
                newChannel.setSearchCount(existingChannel.getSearchCount());
            }

            channelRepository.upsertChannel(newChannel);
        }
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


    @Transactional(readOnly = true)
    public List<ChannelResponseDto> findRandomChannels() {
        List<Channel> channels = channelRepository.findRandomTopChannels();

        return ChannelResponseDto.ChannelToDto(channels);
    }

    @Transactional(readOnly = true)
    public List<ChannelResponseDto> getTopChannels() {
        log.info("Fetching top channels from DB."); // Simplified log
        List<Channel> top10Channels = channelRepository.findTop10ByOrderBySearchCountDesc();
        return ChannelResponseDto.ChannelToDto(top10Channels);
    }
}
