package com.springexample.mnistweb.service;

import com.springexample.mnistweb.dto.PredictResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class ImageClassifierService {

    // RestClient : OpenAPI와 같은 외부 웹요청 처리기
    private final RestClient restClient;

    public ImageClassifierService(RestClient classifierRestClient) {
        this.restClient = classifierRestClient;
    }

    /** GET /health — 모델 서버 상태 확인 */
    public boolean isHealthy() {
        try {
            Map<String, Object> body = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return body != null && "ok".equals(body.get("status"));
        } catch (RestClientException e) {
            return false;
        }
    }

    /** POST /predict — 이미지 분류 요청 */
    public PredictResponse classify(MultipartFile image) {
        String contentType = image.getContentType() != null
                ? image.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // Python의 files={"file": ("test.jpg", f, "image/jpeg")} 와 같은 역할
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", image.getResource())                 // 파트 이름 "file" = FastAPI 파라미터명
                .contentType(MediaType.parseMediaType(contentType));

        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()                                     // 4xx/5xx → RestClientResponseException
                .body(PredictResponse.class);
    }
}