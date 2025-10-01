package youtube.youtube_api_practice.repository.channel;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import youtube.youtube_api_practice.domain.Channel;

import java.util.List;
import java.util.Set;


public interface ChannelRepository extends JpaRepository<Channel, String>, ChannelJdbcRepository {

    // channelId 가 주어진 Set 안에 포함된 Channel들을 반환
    List<Channel> findByIdInOrderBySubscriberCountDesc(Set<String> channelIds);

    // search_count 기준으로 상위 10개 채널을 가져오는 메서드
    List<Channel> findTop10ByOrderBySearchCountDesc();


    @Query("select c from Channel c where c.subscriberCount > :subscriberCount and c.lastSelectedAt is null order by c.subscriberCount desc")
    List<Channel> findTopChannels(@Param("subscriberCount") Long subscriberCount, Pageable pageable);

}
