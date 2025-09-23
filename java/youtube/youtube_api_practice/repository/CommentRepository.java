package youtube.youtube_api_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import youtube.youtube_api_practice.domain.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.video v WHERE v.id IN :videoIds ORDER BY c.likeCount DESC")
    List<Comment> findAllByVideoIdInOrderByLikeCountDesc(@Param("videoIds") List<String> videoIds);



    @Query("SELECT c FROM Comment c JOIN FETCH c.video v JOIN FETCH v.channel ch WHERE ch.id = :channelId ORDER BY c.likeCount")
    List<Comment> findByChannelOrderByLikeCount(@Param("channelId") String channelId);


}
