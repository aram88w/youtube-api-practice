package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.service.ChannelService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequestMapping("/youtube/api")
@RestController
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping("/channel")
    public CompletableFuture<List<ChannelResponseDto>> getChannels(@RequestParam("search") String search) {
        log.info("getChannels {}", search);
        return channelService.getChannelIds(search);
    }

    @GetMapping("/channel/detail/{channelId}")
    public ChannelResponseDto getChannelDetails(@PathVariable String channelId) {
        log.info("getChannelDetails {}", channelId);
        return channelService.getChannelDetails(channelId);
    }

    @GetMapping("/channel/recommendations")
    public List<ChannelResponseDto> recommendChannels() {
        log.info("recommendChannels");
        return channelService.findRandomChannels();
    }

    @GetMapping("/top-channels")
    public List<ChannelResponseDto> getTopChannels() {
        log.info("getTopChannels request received.");
        return channelService.getTopChannels();
    }
}
