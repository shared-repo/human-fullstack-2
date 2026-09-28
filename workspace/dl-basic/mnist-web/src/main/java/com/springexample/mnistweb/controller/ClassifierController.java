package com.springexample.mnistweb.controller;

import com.springexample.mnistweb.dto.PredictResponse;
import com.springexample.mnistweb.service.ImageClassifierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Controller
public class ClassifierController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final ImageClassifierService classifierService;

    public ClassifierController(ImageClassifierService classifierService) {
        this.classifierService = classifierService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("serverUp", classifierService.isHealthy());
        return "index";
    }

    @PostMapping("/classify")
    public String classify(@RequestParam("file") MultipartFile file, Model model) throws IOException {
        model.addAttribute("serverUp", classifierService.isHealthy());

        // 1) 클라이언트 측 1차 검증 (FastAPI에서도 다시 검증함)
        if (file.isEmpty()) {
            model.addAttribute("error", "이미지 파일을 선택하세요.");
            return "index";
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            model.addAttribute("error", "JPG/PNG/WEBP 이미지만 업로드할 수 있습니다.");
            return "index";
        }

        // 2) FastAPI 호출 및 예외 처리
        try {
            PredictResponse result = classifierService.classify(file);

            model.addAttribute("result", result);
            // 확률 내림차순 정렬 (화면 표시용)
            model.addAttribute("scores", result.allScores().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .toList());
            // 업로드한 이미지를 결과와 함께 미리보기 (Base64 data URI)
            model.addAttribute("imageData", "data:" + file.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes()));

        } catch (RestClientResponseException e) {        // FastAPI가 4xx/5xx 응답
            model.addAttribute("error", "API 오류 (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {            // 연결 실패·타임아웃
            model.addAttribute("error", "FastAPI 서버에 연결할 수 없습니다. uvicorn 실행 여부를 확인하세요.");
        }
        return "index";
    }
}
