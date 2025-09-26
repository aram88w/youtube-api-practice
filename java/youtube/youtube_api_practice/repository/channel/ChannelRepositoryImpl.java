package youtube.youtube_api_practice.repository.channel;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import youtube.youtube_api_practice.domain.Channel;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ChannelRepositoryImpl implements ChannelJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsertChannel(Channel channel) {
        String sql = """
            INSERT INTO channel (channel_id, channel_name, thumbnail_url, subscriber_count, description, uploads_playlist_id, last_selected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                channel_name = VALUES(channel_name),
                thumbnail_url = VALUES(thumbnail_url),
                subscriber_count = VALUES(subscriber_count),
                description = VALUES(description),
                uploads_playlist_id = VALUES(uploads_playlist_id),
                last_selected_at = VALUES(last_selected_at)
        """;

        jdbcTemplate.update(sql,
                channel.getId(),
                channel.getName(),
                channel.getThumbnailUrl(),
                channel.getSubscriberCount(),
                channel.getDescription(),
                channel.getUploadsPlaylistId(),
                channel.getLastSelectAt()
        );
    }
}
