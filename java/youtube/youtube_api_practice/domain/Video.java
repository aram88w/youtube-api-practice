package youtube.youtube_api_practice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video implements Persistable<String> {

    @Id
    @Column(name = "video_id")
    private String id;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

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
    public Video(String id, String title, LocalDateTime publishedAt, String thumbnailUrl, Channel channel) {
        this.id = id;
        this.title = title;
        this.publishedAt = publishedAt;
        this.thumbnailUrl = thumbnailUrl;
        this.channel = channel;
    }

    //==연관관계 편의 메서드==//
    public void addComment(Comment comment) {
        comments.add(comment);
    }
}