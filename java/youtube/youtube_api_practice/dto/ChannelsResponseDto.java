package youtube.youtube_api_practice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ChannelsResponseDto {

    private final String id;
    private final String name;
    private final String description;
    private final String thumbnailUrl;

    @Builder
    public ChannelsResponseDto(String id, String name, String description, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

}
