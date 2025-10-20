package youtube.youtube_api_practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.SearchCache;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.exception.ChannelNotFoundException;
import youtube.youtube_api_practice.client.YoutubeProvider;
import youtube.youtube_api_practice.repository.SearchCacheRepository;
import youtube.youtube_api_practice.repository.channel.ChannelRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final YoutubeProvider youtubeProvider;
    private final ChannelRepository channelRepository;
    private final SearchCacheRepository searchCacheRepository;

    private final LevenshteinDistance ld = new LevenshteinDistance();

    @Async
    @Transactional
    public CompletableFuture<List<ChannelResponseDto>> getChannelIds(String search) {
        log.info("getChannelIds {}", search);

        // 1. DB에서 검색어 캐시 조회
        Optional<SearchCache> cacheOpt = searchCacheRepository.findById(search);
        SearchCache cache;


        // 2-1. 처음 검색하는 검색어인 경우
        if (cacheOpt.isEmpty()) {
            log.info("처음 검색하는 경우");
            Optional<SearchCache> similarCacheOpt = searchCacheRepository.findAll().stream()
                    .filter(sc -> isSimilar(sc.getKeyword(), search))
                    .findFirst();

            if (similarCacheOpt.isPresent()) {
                log.info("연관 키워드 있는 경우 : {}", similarCacheOpt.get().getKeyword());
                cache = similarCacheOpt.get();
            } else { // 연관 키워드도 없으면 api 요청
                log.info("연관 키워드 없는 경우");
                Set<String> channelIds = youtubeProvider.fetchChannelIds(search);
                cache = new SearchCache();
                cache.setKeyword(search);
                cache.setLastSearchedAt(LocalDateTime.now());
                cache.setChannelIds(channelIds);

                searchCacheRepository.save(cache);
                saveChannels(channelIds);
            }

        } else { // 이미 검색한 적이 있는 경우
            log.info("검색한 적이 있는 경우");
            cache = cacheOpt.get();

            // 2-2. 마지막 갱신이 오래되었으면 API로 갱신
            if (cache.getLastSearchedAt().isBefore(LocalDateTime.now().minusHours(200))) {
                Set<String> oldIds = cache.getChannelIds();
                Set<String> newIds = youtubeProvider.fetchChannelIds(search);

                // 없어진 채널들 삭제 작업
                oldIds.removeAll(newIds);
                channelRepository.deleteAllById(oldIds);

                // 새로 받은 채널 저장
                cache.setChannelIds(newIds);
                cache.setLastSearchedAt(LocalDateTime.now());

                searchCacheRepository.save(cache);
                saveChannels(newIds);
            }
        }
        // 3. 캐시에서 채널 ID 가져와 DB 조회
        List<Channel> channels = channelRepository.findByIdInOrderBySubscriberCountDesc(cache.getChannelIds());
        return CompletableFuture.completedFuture(ChannelResponseDto.ChannelToDto(channels));
    }


    public void saveChannels(Set<String> channelIds) {
        log.info("saveChannels {}", channelIds);

        // 1. 기존 채널 정보를 Map으로 한번에 조회
        Map<String, Channel> existingChannelMap = channelRepository.findAllById(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));

        // 2. API 호출 및 upsert 진행
        for (String channelId : channelIds) {
            Channel newChannel = youtubeProvider.fetchChannel(channelId);
            Channel existingChannel = existingChannelMap.get(channelId);

            // 3. 기존 채널 정보가 있으면 lastSelectAt, searchCount, commentStatus 값을 유지
            if (existingChannel != null) {
                newChannel.setLastSelectAt(existingChannel.getLastSelectedAt());
                newChannel.setSearchCount(existingChannel.getSearchCount());
                newChannel.setCommentStatus(existingChannel.getCommentStatus());
            }

            channelRepository.upsertChannel(newChannel);
        }
    }

    public boolean isSimilar(String a, String b) {
        int distance = ld.apply(a, b);
        int maxLen = Math.max(a.length(), b.length());
        double similarity = 1 - (double) distance / maxLen;

        if (maxLen <= 4) {
            return distance <= 1;
        } else {
            return similarity >= 0.85;
        }
    }


    @Transactional(readOnly = true)
    public ChannelResponseDto getChannelDetails(String channelId) {
        log.info("getChannelDetails {}", channelId);
        return channelRepository.findById(channelId)
                .map(channel -> ChannelResponseDto.builder()
                        .id(channel.getId())
                        .name(channel.getName())
                        .description(channel.getDescription())
                        .thumbnailUrl(channel.getThumbnailUrl())
                        .subscriberCount(channel.getSubscriberCount())
                        .commentStatus(channel.getCommentStatus())
                        .build())
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found id: " + channelId));
    }


    @Transactional(readOnly = true)
    public List<ChannelResponseDto> findRandomChannels() {
        log.info("findRandomChannels");
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
