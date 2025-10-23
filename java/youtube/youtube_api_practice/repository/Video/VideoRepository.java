package youtube.youtube_api_practice.repository.Video;

import org.springframework.data.jpa.repository.JpaRepository;
import youtube.youtube_api_practice.domain.Video;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, String>, VideoJdbcRepository {
}
