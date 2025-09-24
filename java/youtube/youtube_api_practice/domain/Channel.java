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

    @Transient
    private boolean isNew = true;

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
    private LocalDateTime lastSelectAt;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "subscriber_count")
    private Long subscriberCount; // 구독자 수


    @Builder
    public Channel(String id, String uploadsPlaylistId, String name, String description, int searchCount,
                   LocalDateTime lastSelectAt, String thumbnailUrl, Long subscriberCount) {
        this.id = id;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.name = name;
        this.description = description;
        this.searchCount = searchCount;
        this.lastSelectAt = lastSelectAt;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
    }

    public void updateChannelInfo(String name, String description, String thumbnailUrl,
                                  String uploadsPlaylistId, LocalDateTime lastSelectAt, Long subscriberCount) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.lastSelectAt = lastSelectAt;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.subscriberCount = subscriberCount;
    }

    public void setLastSelectAt(LocalDateTime lastSelectAt) {
        this.lastSelectAt = lastSelectAt;
    }

    //==연관관계 편의 메서드==//
    public void addVideo(Video video) {
        videos.add(video);
    }

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