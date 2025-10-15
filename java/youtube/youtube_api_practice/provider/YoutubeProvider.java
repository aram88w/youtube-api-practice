package youtube.youtube_api_practice.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import youtube.youtube_api_practice.client.YoutubeApi;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.exception.ChannelNotFoundException;
import youtube.youtube_api_practice.exception.YoutubeApiFailedException;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeProvider {

    private final YoutubeApi youtubeApi;

    public Set<String> fetchChannelIds(String search) {
        log.info("fetchChannelIds {}", search);

        try {
            List<String> result = youtubeApi.getChannelIdsBySearch(search);
            if (result == null) {
                throw new YoutubeApiFailedException("Failed to call Youtube API for searchChannelIds: " + search);
            }
            return new HashSet<>(result);
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new YoutubeApiFailedException("Failed to call Youtube API for searchChannelIds: " + search, e);
        }
    }

    public Channel fetchChannel(String channelId) {
        log.info("fetchChannel {}", channelId);

        try {
            Channel channel = youtubeApi.getChannelById(channelId);
            if (channel == null) {
                throw new ChannelNotFoundException("Failed to call Youtube API for channel: " + channelId);
            }
            return channel;
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new YoutubeApiFailedException("Failed to call Youtube API for channel: " + channelId, e);
        }
    }

    public List<Video> fetchVideos(Channel channel, int limit) {
        log.info("fetchVideos {}, limit {}", channel, limit);

        try {
            List<Video> videos = youtubeApi.getVideosByChannel(channel, limit);
            if (videos == null) {
                throw new YoutubeApiFailedException("Failed to call Youtube API for videos: " + channel.getId());
            }
            return videos;
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new YoutubeApiFailedException("Failed to call Youtube API for videos: " + channel.getId(), e);
        }
    }

    public List<Comment> fetchComments(Video video, int limit) {
        log.info("fetchComments {}, limit {}", video, limit);

        try {
            List<Comment> comments = youtubeApi.getCommentsByVideo(video, limit);
            if (comments == null) {
                throw new YoutubeApiFailedException("Failed to call Youtube API for comments: " + video.getId());
            }
            return comments;
        } catch (WebClientResponseException.Forbidden e) {
            log.warn("댓글이 비활성화된 동영상입니다. videoId={}, 응답 본문: {}", video.getId(), e.getResponseBodyAsString());
            return new ArrayList<>();
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new YoutubeApiFailedException("Failed to call Youtube API for comments: " + video.getId(), e);
        }
    }

    public ReplyResponseDto fetchReply(String commentId, String pageToken) {
        log.info("fetchReply {} pageToken {}", commentId, pageToken);

        try {
            ReplyResponseDto responseDto = youtubeApi.getRepliesByComment(commentId, pageToken);

            if (responseDto == null) {
                throw new YoutubeApiFailedException("Failed to call Youtube API for replies: " + commentId);
            }

            return responseDto;
        } catch (WebClientRequestException | WebClientResponseException e) {
            throw new YoutubeApiFailedException("Failed to call Youtube API for replies: " + commentId, e);
        }
    }
}
