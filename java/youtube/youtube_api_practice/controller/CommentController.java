package youtube.youtube_api_practice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import youtube.youtube_api_practice.dto.CommentResponseDto;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.service.CommentService;

import java.util.concurrent.CompletableFuture;

import jakarta.servlet.http.HttpServletRequest; // 새로 추가

@Slf4j
@RequestMapping("/youtube/api")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // /youtube/api/comments/UCUj6rrhMTR9pipbAWBAMvUQ
    @GetMapping("/comments/{channelId}")
    public CompletableFuture<Page<CommentResponseDto>> getComments(@PathVariable String channelId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                HttpServletRequest request) { // HttpServletRequest 파라미터 추가
        log.info("getComments {} page={} size={}", channelId, page, size);

        return commentService.getComments(channelId, page, size, request); // service 메서드 호출 시 request 전달
    }

    @GetMapping("/comments/{commentId}/replies")
    public ReplyResponseDto getReplies(@PathVariable String commentId,
                                       @RequestParam(required = false) String pageToken) {
        log.info("getReplies for commentId {} with pageToken {}", commentId, pageToken);
        return commentService.getReplies(commentId, pageToken);
    }

}
