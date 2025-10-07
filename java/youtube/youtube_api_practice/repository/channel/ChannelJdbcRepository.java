package youtube.youtube_api_practice.repository.channel;

import youtube.youtube_api_practice.domain.Channel;

import java.util.List;

public interface ChannelJdbcRepository {
    void upsertChannel(Channel channel);

    List<Channel> findRandomTopChannels();
}
