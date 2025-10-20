package youtube.youtube_api_practice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import youtube.youtube_api_practice.domain.Channel;
import youtube.youtube_api_practice.domain.Comment;
import youtube.youtube_api_practice.domain.Video;
import youtube.youtube_api_practice.dto.ReplyResponseDto;
import youtube.youtube_api_practice.exception.ChannelNotFoundException;
import youtube.youtube_api_practice.exception.QuotaExceededException;
import youtube.youtube_api_practice.exception.YoutubeApiFailedException;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeProvider {

    private final YoutubeApi youtubeApi;
    private final ObjectMapper objectMapper;

    public Set<String> fetchChannelIds(String search) {
        log.info("fetchChannelIds {}", search);

        try {
            List<String> result = youtubeApi.getChannelIdsBySearch(search);
            if (result == null) {
                throw new YoutubeApiFailedException("Failed to call Youtube API for searchChannelIds: " + search);
            }
            return new HashSet<>(result);
        } catch (WebClientRequestException e) {
            throw new YoutubeApiFailedException("Failed to request Youtube API for searchChannelIds: " + search, e);
        } catch (WebClientResponseException e) {
            String reason = getReasonFromError(e);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "quotaExceeded".equals(reason)) {
                log.warn("유튜브 쿼터를 모두 사용함");
                throw new QuotaExceededException("YouTube API quota exceeded");
            }

            throw new YoutubeApiFailedException("Failed to response Youtube API for searchChannelIds: " + search, e);
        }
    }

//    {
//        "error":{
//          "code":404,
//          "message": "The playlist identified with the request's \u003ccode\u003eplaylistId\u003c/code\u003e parameter cannot be found.",
//          "errors": [
//              {
//                  "message":
//                  "The playlist identified with the request's \u003ccode\u003eplaylistId\u003c/code\u003e parameter cannot be found.",
//                  "domain":"youtube.playlistItem",
//                  "reason":"playlistNotFound",
//                  "location":"playlistId",
//                  "locationType":"parameter"
//              }
//          ]
//       }
//    }

    public Channel fetchChannel(String channelId) {
        log.info("fetchChannel {}", channelId);

        try {
            Channel channel = youtubeApi.getChannelById(channelId);
            if (channel == null) {
                throw new ChannelNotFoundException("Channel not found with id: " + channelId);
            }
            return channel;
        } catch (WebClientRequestException e) {
            throw new YoutubeApiFailedException("Failed to request Youtube API for channel: " + channelId, e);
        } catch (WebClientResponseException e) {
            String reason = getReasonFromError(e);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "quotaExceeded".equals(reason)) {
                log.warn("유튜브 쿼터를 모두 사용함");
                throw new QuotaExceededException("YouTube API quota exceeded");
            }

            throw new YoutubeApiFailedException("Failed to response Youtube API for channel: " + channelId, e);
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
        } catch (WebClientRequestException e) {
            throw new YoutubeApiFailedException("Failed to request Youtube API for videos: " + channel.getId(), e);
        } catch (WebClientResponseException e) {
            String reason = getReasonFromError(e);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "quotaExceeded".equals(reason)) {
                log.warn("유튜브 쿼터를 모두 사용함");
                throw new QuotaExceededException("YouTube API quota exceeded");
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && "playlistNotFound".equals(reason)) {
                log.warn("채널에 비디오가 없음");
                return new ArrayList<>();
            }

            throw new YoutubeApiFailedException("Failed to response Youtube API for videos: " + channel.getId(), e);
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
        } catch (WebClientRequestException e) {
            throw new YoutubeApiFailedException("Failed to request Youtube API for comments: " + video.getId(), e);
        } catch (WebClientResponseException e) {
            String reason = getReasonFromError(e);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "commentsDisabled".equals(reason)) {
                log.warn("댓글이 비활성화된 동영상입니다. videoId={}, 응답 본문: {}", video.getId(), e.getResponseBodyAsString());
                return new ArrayList<>();
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && "videoNotFound".equals(reason)) {
                log.warn("존재하지 않는 동영상입니다. videoId={}, 응답 본문: {}", video.getId(), e.getResponseBodyAsString());
                return new ArrayList<>();
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "quotaExceeded".equals(reason)) {
                log.warn("유튜브 쿼터를 모두 사용함");
                throw new QuotaExceededException("YouTube API quota exceeded");
            }

            throw new YoutubeApiFailedException("Failed to response Youtube API for comments: " + video.getId(), e);
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
        } catch (WebClientRequestException e) {
            throw new YoutubeApiFailedException("Failed to request Youtube API for replies: " + commentId, e);
        } catch (WebClientResponseException e) {
            String reason = getReasonFromError(e);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN && "quotaExceeded".equals(reason)) {
                log.warn("유튜브 쿼터를 모두 사용함");
                throw new QuotaExceededException("YouTube API quota exceeded");
            }

            throw new YoutubeApiFailedException("Failed to response Youtube API for replies: " + commentId, e);
        }
    }

    private String getReasonFromError(WebClientResponseException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("error").path("errors").get(0).path("reason").asText();
        } catch (JsonProcessingException | NullPointerException jsonEx) {
            log.warn("JSON 파싱 중 오류 발생");
            return null;
        }
    }
}
