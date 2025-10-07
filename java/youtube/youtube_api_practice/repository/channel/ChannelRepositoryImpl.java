package youtube.youtube_api_practice.repository.channel;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import youtube.youtube_api_practice.domain.Channel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
public class ChannelRepositoryImpl implements ChannelJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsertChannel(Channel channel) {
        String sql = """
        INSERT INTO channel 
        (channel_id, channel_name, thumbnail_url, subscriber_count, description, uploads_playlist_id, last_selected_at, search_count, comment_status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            channel_name = VALUES(channel_name),
            thumbnail_url = VALUES(thumbnail_url),
            subscriber_count = VALUES(subscriber_count),
            description = VALUES(description),
            uploads_playlist_id = VALUES(uploads_playlist_id),
            last_selected_at = VALUES(last_selected_at),
            search_count = VALUES(search_count),
            comment_status = VALUES(comment_status)
    """;

        jdbcTemplate.update(sql,
                channel.getId(),
                channel.getName(),
                channel.getThumbnailUrl(),
                channel.getSubscriberCount(),
                channel.getDescription(),
                channel.getUploadsPlaylistId(),
                channel.getLastSelectedAt(),
                channel.getSearchCount(),
                channel.getCommentStatus().name() // Enum → String
        );
    }


    private final RowMapper<Channel> channelRowMapper = new RowMapper<>() {
        @Override
        public Channel mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Channel.builder()
                    .id(rs.getString("channel_id"))
                    .name(rs.getString("channel_name"))
                    .description(rs.getString("description"))
                    .thumbnailUrl(rs.getString("thumbnail_url"))
                    .subscriberCount(rs.getLong("subscriber_count"))
                    .build();
        }
    };

    public List<Channel> findRandomTopChannels() {
        String sql = """
                SELECT c.*
                FROM channel c
                JOIN (
                    SELECT channel_id
                    FROM channel
                    WHERE subscriber_count >= 10000
                    ORDER BY RAND()
                    LIMIT 20
                ) r ON c.channel_id = r.channel_id
                ORDER BY c.subscriber_count DESC
                """;

        return jdbcTemplate.query(sql, channelRowMapper);
    }
}
