package youtube.youtube_api_practice.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ReplyResponseDto {

    private final List<ReplyCommentDto> replyComments = new ArrayList<>();
    private final String nextPageToken;

    public ReplyResponseDto(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
