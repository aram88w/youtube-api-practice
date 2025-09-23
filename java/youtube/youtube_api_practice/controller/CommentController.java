package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.service.CommentService;

import java.util.List;

@RequestMapping("/youtube/api")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comments/{name}")
    public List<CommentResponseDto> getComments(@PathVariable String name) {
        String channelId = commentService.getChannelId(name);
        return commentService.getComments(channelId);
    }
}
