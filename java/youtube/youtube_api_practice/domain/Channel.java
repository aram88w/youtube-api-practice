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
@Table(name = "channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel implements Persistable<String> {

    @Id
    @Column(name = "channel_id")
    private String id;

    @Column(name = "uploads_playlist_id")
    private String uploadsPlaylistId;

    @Column(name = "channel_name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos = new ArrayList<>();

    @Column(name = "search_count", nullable = false)
    private int searchCount;

    @Column(name = "last_selected_at")
    private LocalDateTime lastSelectedAt;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "subscriber_count")
    private Long subscriberCount; // 구독자 수

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status", nullable = false, length = 50)
    private CommentStatus commentStatus = CommentStatus.COMMENT_NONE;


    @Builder
    public Channel(String id, String uploadsPlaylistId, String name, String description, int searchCount,
                   LocalDateTime lastSelectedAt, String thumbnailUrl, Long subscriberCount, CommentStatus commentStatus) {
        this.id = id;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.name = name;
        this.description = description;
        this.searchCount = searchCount;
        this.lastSelectedAt = lastSelectedAt;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.commentStatus = commentStatus;
    }


    public void setLastSelectAt(LocalDateTime lastSelectedAt) {
        this.lastSelectedAt = lastSelectedAt;
    }

    public void incrementSearchCount() {
        this.searchCount++;
    }

    public void setSearchCount(int searchCount) {
        this.searchCount = searchCount;
    }

    public void setCommentStatus(CommentStatus commentStatus) {
        this.commentStatus = commentStatus;
    }

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
}