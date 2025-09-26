package youtube.youtube_api_practice.repository.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import youtube.youtube_api_practice.domain.Channel;

import java.util.List;
import java.util.Set;


public interface ChannelRepository extends JpaRepository<Channel, String>, ChannelJdbcRepository {

    // channelId 가 주어진 Set 안에 포함된 Channel들을 반환
    List<Channel> findByIdInOrderBySubscriberCountDesc(Set<String> channelIds);
}
