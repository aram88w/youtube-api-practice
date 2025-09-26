package youtube.youtube_api_practice.repository.Video;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import youtube.youtube_api_practice.domain.Video;

import java.util.List;

@RequiredArgsConstructor
public class VideoRepositoryImpl implements VideoJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void upsertVideos(List<Video> videos) {
        String sql = """
        INSERT INTO video (video_id, title, published_at, thumbnail_url, channel_id)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            title = VALUES(title),
            published_at = VALUES(published_at),
            thumbnail_url = VALUES(thumbnail_url),
            channel_id = VALUES(channel_id)
    """;

        jdbcTemplate.batchUpdate(sql, videos, videos.size(),
                (ps, video) -> {
                    ps.setString(1, video.getId());
                    ps.setString(2, video.getTitle());
                    ps.setObject(3, video.getPublishedAt()); // LocalDateTime → JDBC 자동 변환
                    ps.setString(4, video.getThumbnailUrl());
                    ps.setString(5, video.getChannel().getId());
                }
        );
    }

}


