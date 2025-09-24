package youtube.youtube_api_practice.dto;

import lombok.Builder;
import lombok.Getter;
import youtube.youtube_api_practice.domain.Comment;

import java.time.LocalDateTime;

@Getter
public class CommentResponseDto {

    // Comment fields
    private final String commentId;
    private final String content;
    private final String authorName;
    private final String authorThumbnailUrl;
    private final int likeCount;
    private final LocalDateTime publishedAt;

    // Video fields
    private final String videoId;
    private final String videoTitle;
    private final String videoThumbnailUrl;

    @Builder
    public CommentResponseDto(Comment comment) {
        this.commentId = comment.getId();
        this.content = comment.getContent();
        this.authorName = comment.getAuthorName();
        this.authorThumbnailUrl = comment.getAuthorThumbnailUrl();
        this.likeCount = comment.getLikeCount();
        this.publishedAt = comment.getPublishedAt();
        this.videoId = comment.getVideo().getId();
        this.videoTitle = comment.getVideo().getTitle();
        this.videoThumbnailUrl = comment.getVideo().getThumbnailUrl();
    }
}
