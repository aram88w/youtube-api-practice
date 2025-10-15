package youtube.youtube_api_practice.exception;

public class ChannelNotFoundException extends RuntimeException {
    public ChannelNotFoundException(String channelId) {
        super("Channel not found with id: " + channelId);
    }
}
