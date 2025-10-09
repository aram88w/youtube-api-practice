package youtube.youtube_api_practice.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component
public class ImageClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<byte[]> fetchImage(String url) {
        try {
            URI uri = new URI(url);

            HttpHeaders headers = new HttpHeaders();
            // 1. Referer 헤더 추가 (핫링킹 방지)
            headers.set("Referer", "https://www.youtube.com/");
            // 2. 일반적인 브라우저의 User-Agent 헤더 추가 (봇 차단 방지)
            // headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);

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