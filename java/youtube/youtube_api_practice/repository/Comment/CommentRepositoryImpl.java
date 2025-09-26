package youtube.youtube_api_practice.repository.Comment;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import youtube.youtube_api_practice.domain.Comment;

import java.util.List;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void upsertComments(List<Comment> comments) {
        String sql = """
        INSERT INTO comment (
            comment_id,
            author_id,
            author_name,
            author_thumbnail_url,
            content,
            published_at,
            like_count,
            reply_count,
            video_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            author_id = VALUES(author_id),
            author_name = VALUES(author_name),
            author_thumbnail_url = VALUES(author_thumbnail_url),
            content = VALUES(content),
            published_at = VALUES(published_at),
            like_count = VALUES(like_count),
            reply_count = VALUES(reply_count),
            video_id = VALUES(video_id)
    """;

        jdbcTemplate.batchUpdate(sql, comments, comments.size(),
                (ps, comment) -> {
                    ps.setString(1, comment.getId());
                    ps.setString(2, comment.getAuthorId());
                    ps.setString(3, comment.getAuthorName());
                    ps.setString(4, comment.getAuthorThumbnailUrl());
                    ps.setString(5, comment.getContent());
                    ps.setObject(6, comment.getPublishedAt()); // LocalDateTime → DATETIME 변환
                    ps.setInt(7, comment.getLikeCount());
                    ps.setInt(8, comment.getReplyCount());
                    ps.setString(9, comment.getVideo().getId());
                }
        );
    }


}
