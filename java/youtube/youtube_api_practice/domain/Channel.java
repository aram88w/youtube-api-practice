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

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;


    @Builder
    public Channel(String id, String uploadsPlaylistId, String name, String description, int searchCount,
                   LocalDateTime lastSearchedAt, String thumbnailUrl) {
        this.id = id;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.name = name;
        this.description = description;
        this.searchCount = searchCount;
        this.lastSearchedAt = lastSearchedAt;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateChannelInfo(String name, String description, String thumbnailUrl, LocalDateTime lastSearchedAt) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.lastSearchedAt = lastSearchedAt;
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