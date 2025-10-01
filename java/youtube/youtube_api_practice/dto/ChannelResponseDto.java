package youtube.youtube_api_practice.dto;

import lombok.Builder;
import lombok.Getter;
import youtube.youtube_api_practice.domain.Channel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
public class ChannelResponseDto {

    private final String id;
    private final String name;
    private final String description;
    private final String thumbnailUrl;
    private final Long subscriberCount;

    @Builder
    public ChannelResponseDto(String id, String name, String description, String thumbnailUrl, Long subscriberCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
    }

    // List<Channel> -> List<ChannelResponseDto> 변환
    public static List<ChannelResponseDto> ChannelToDto(List<Channel> channels) {
        List<ChannelResponseDto> result = new ArrayList<>();

        for (Channel channel : channels) {
            result.add(ChannelResponseDto.builder()
                    .id(channel.getId())
                    .name(channel.getName())
                    .description(channel.getDescription())
                    .thumbnailUrl(channel.getThumbnailUrl())
                    .subscriberCount(channel.getSubscriberCount())
                    .build());
        }
        return result;
    }

}
