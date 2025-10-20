package youtube.youtube_api_practice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(ChannelNotFoundException.class)
    public ResponseEntity<ErrorResponse> channelNotFoundHandler(ChannelNotFoundException e) {
        log.warn("handleChannelNotFound : {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("CHANNEL_NOT_FOUND", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(YoutubeApiFailedException.class)
    public ResponseEntity<ErrorResponse> youtubeApiFailedHandler(YoutubeApiFailedException e) {
        log.error("handleYoutubeApiFailed : {}", e.getMessage(), e.getCause());
        ErrorResponse response = new ErrorResponse("YOUTUBE_API_FAILED", "Failed to communicate with YouTube API.");
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> quotaExceededHandler(QuotaExceededException e) {
        log.error("handleQuotaExceeded : {}", e.getMessage(), e.getCause());
        ErrorResponse response = new ErrorResponse("QUOTA_EXCEEDED", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception e) {

        log.error("handleException : {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("EXCEPTION", "An unexpected internal server error occurred.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
