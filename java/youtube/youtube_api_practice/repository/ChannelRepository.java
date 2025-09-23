package youtube.youtube_api_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import youtube.youtube_api_practice.domain.Channel;


public interface ChannelRepository extends JpaRepository<Channel, String> {
}
