package youtube.youtube_api_practice.repository.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import youtube.youtube_api_practice.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, String>, CommentJdbcRepository {

    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.video v JOIN FETCH v.channel ch " +
            "WHERE ch.id = :channelId ORDER BY c.likeCount DESC",
           countQuery = "SELECT count(c) FROM Comment c WHERE c.video.channel.id = :channelId")
    Page<Comment> findByChannelOrderByLikeCountDesc(@Param("channelId") String channelId, Pageable pageable);

}
