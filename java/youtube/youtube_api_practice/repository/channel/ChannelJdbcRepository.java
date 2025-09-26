package youtube.youtube_api_practice.repository.channel;

import youtube.youtube_api_practice.domain.Channel;

public interface ChannelJdbcRepository {
    void upsertChannel(Channel channel);
}
