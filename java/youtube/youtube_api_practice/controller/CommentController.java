package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.dto.ChannelsResponseDto;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.service.CommentService;

import java.util.List;

@Slf4j
@RequestMapping("/youtube/api")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final YoutubeApi youtubeApi;

    @GetMapping("/channel/{search}")
    public List<ChannelsResponseDto> getChannels(@PathVariable String search) {
        log.info("getChannels {}", search);
        return youtubeApi.getChannelIdBySearch(search);
    }

    @GetMapping("/comments/{channelId}")
    public List<CommentResponseDto> getComments(@PathVariable String channelId) {
        log.info("getComments {}", channelId);
        return commentService.getComments(channelId);
    }
}
