package youtube.youtube_api_practice.repository.Video;

import youtube.youtube_api_practice.domain.Video;

import java.util.List;

public interface VideoJdbcRepository {
    void upsertVideos(List<Video> videos);
}
