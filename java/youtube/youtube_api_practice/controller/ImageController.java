package youtube.youtube_api_practice.controller;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Controller
@RequestMapping("/youtube/api")
public class ImageController {

    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage(@RequestParam("url") String url) {
        try {
            // 헤더 주입 및 기타 문제를 방지하기 위해 URL 유효성을 검사하고 URI 객체를 생성합니다.
            URI uri = new URI(url);

            RestTemplate restTemplate = new RestTemplate();
            // 외부 이미지 URL로 요청을 보냅니다.
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, null, byte[].class);

            // 요청이 성공했는지 확인합니다.
            if (response.getStatusCode() == HttpStatus.OK) {
                // 클라이언트로 보낼 응답 헤더를 준비합니다.
                HttpHeaders headers = new HttpHeaders();
                // 원본 응답에서 콘텐츠 유형(Content-Type)을 복사합니다.
                if (response.getHeaders().getContentType() != null) {
                    headers.setContentType(response.getHeaders().getContentType());
                } else {
                    // 콘텐츠 유형이 없는 경우 기본값으로 설정합니다.
                    headers.setContentType(MediaType.IMAGE_JPEG);
                }
                // 콘텐츠 길이를 설정합니다.
                headers.setContentLength(response.getBody().length);

                // 올바른 헤더와 함께 이미지 바이트를 반환합니다.
                return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            }
        } catch (URISyntaxException e) {
            // 잘못된 URL 구문에 대해 오류를 기록하고 잘못된 요청(Bad Request) 응답을 반환합니다.
            System.err.println("Invalid URL syntax: " + url);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // 기타 예외(예: 네트워크 오류, 이미지 서버의 4xx/5xx 응답)를 기록합니다.
            System.err.println("Failed to fetch image from url: " + url + " - " + e.getMessage());
        }

        // 대체 응답으로 찾을 수 없음(Not Found)을 반환합니다.
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
