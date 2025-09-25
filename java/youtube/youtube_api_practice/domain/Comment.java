package youtube.youtube_api_practice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment implements Persistable<String> {

    @Id
    @Column(name = "comment_id")
    private String id;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "author_thumbnail_url", columnDefinition = "TEXT")
    private String authorThumbnailUrl;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "reply_count")
    private int replyCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Builder
    public Comment(String id, String authorId, String authorName, String authorThumbnailUrl, String content, LocalDateTime publishedAt, int likeCount, int replyCount, Video video) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorThumbnailUrl = authorThumbnailUrl;
        this.content = content;
        this.publishedAt = publishedAt;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.video = video;
    }

}