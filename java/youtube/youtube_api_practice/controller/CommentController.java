package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import youtube.youtube_api_practice.YoutubeApi;
import youtube.youtube_api_practice.dto.ChannelResponseDto;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.service.CommentService;

import java.util.List;

@Slf4j
@RequestMapping("/youtube/api")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/channel/{search}")
    public List<ChannelResponseDto> getChannels(@PathVariable String search) {
        log.info("getChannels {}", search);
        return commentService.getChannelIds(search);
    }

    @GetMapping("/channel/detail/{channelId}")
    public ChannelResponseDto getChannelDetails(@PathVariable String channelId) {
        return commentService.getChannelDetails(channelId);
    }

    @GetMapping("/comments/{channelId}")
    public Page<CommentResponseDto> getComments(@PathVariable String channelId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        log.info("getComments {} page={} size={}", channelId, page, size);
        return commentService.getComments(channelId, page, size);
    }

    @GetMapping("/comments/{commentId}/replies")
    public ReplyResponseDto getReplies(@PathVariable String commentId,
                                       @RequestParam(required = false) String pageToken) {
        log.info("getReplies for commentId {} with pageToken {}", commentId, pageToken);
        return commentService.getReplies(commentId, pageToken);
    }
}
