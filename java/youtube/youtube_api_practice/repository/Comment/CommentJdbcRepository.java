package youtube.youtube_api_practice.repository.Comment;

import youtube.youtube_api_practice.domain.Comment;

import java.util.List;

public interface CommentJdbcRepository {
    void upsertComments(List<Comment> comments);
}
