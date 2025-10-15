package youtube.youtube_api_practice.exception;


public class YoutubeApiFailedException extends RuntimeException {
    public YoutubeApiFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public YoutubeApiFailedException(String message) {
        super(message);
    }
}
