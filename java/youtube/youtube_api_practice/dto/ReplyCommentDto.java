package youtube.youtube_api_practice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReplyCommentDto {

    private final String name;
    private final String ThumbnailUrl;
    private final String content;
    private final int likeCount;
    private final LocalDateTime createdAt;

    @Builder
    public ReplyCommentDto(String name, String ThumbnailUrl, String content, LocalDateTime publishedAt, int likeCount, LocalDateTime createdAt) {
        this.name = name;
        this.ThumbnailUrl = ThumbnailUrl;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }
}
