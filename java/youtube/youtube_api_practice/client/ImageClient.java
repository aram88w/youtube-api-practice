package youtube.youtube_api_practice.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component; // Changed to Component
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional; // Import Optional

@Component // Changed from @Service to @Component, or could be @Service
public class ImageClient {

    private final RestTemplate restTemplate = new RestTemplate(); // Make RestTemplate a field

    public Optional<byte[]> fetchImage(String url) {
        try {
            URI uri = new URI(url);

            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, null, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL syntax in ImageClient: " + url + " - " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("HTTP error fetching image in ImageClient: " + url + " - Status: " + e.getStatusCode() + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching image in ImageClient: " + url + " - " + e.getMessage());
        }
        return Optional.empty();
    }
}