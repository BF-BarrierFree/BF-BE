package com.barrierfree.bf.taxi.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {

    private final WebClient openRouterWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openrouter.api.base-url}")
    private String baseUrl;

    @Value("${openrouter.api.api-key}")
    private String apiKey;

    @Value("${openrouter.api.model}")
    private String model;

    public String estimateFare(
            double distanceKm,
            String startAddr,
            String endAddr,
            String basicFareExpln,
            String extraFareExpln) {
        String prompt = String.format(
                "출발지: %s\n도착지: %s\n이동 거리는 %.1f km입니다.\n"
                        + "기본요금 규정: %s\n추가요금 규정: %s\n\n"
                        + "이 규정을 바탕으로 예상 요금을 원 단위로 계산해서 과정 없이 간략하게 결과 금액만(예: '약 2,500원') 대답해줘. "
                        + "만약 출발지와 도착지의 행정구역(시/도, 시/군/구)이 달라 관외(시계외) 할증 규정이 적용되어야 한다면 이를 스스로 판단해서 결과에 반영해줘.",
                startAddr, endAddr, distanceKm, basicFareExpln, extraFareExpln);
        return callOpenRouter(prompt, 256);
    }

    public String generateFormSchema(String targetExpln, String notice) {
        String prompt = String.format(
                "장애인 콜택시 예약에 실제로 필요한 입력 폼만 만들어줘.\n"
                        + "센터 규정의 모든 자격요건을 폼으로 옮기지 말고, 예약 접수에 반드시 필요한 정보만 골라.\n"
                        + "주소/요청사항/기타 설명은 type=text, 인원/수량은 type=number를 사용해. "
                        + "select/radio는 휠체어 사용 여부처럼 선택지가 명확하고 4개 이하일 때만 사용해. "
                        + "법적 자격 확인, 진단서 내용, 서류 제출 여부처럼 예약 전에 센터가 별도로 확인할 사항은 폼에서 제외해.\n"
                        + "이용대상: %s\n예약안내사항: %s\n\n"
                        + "출발지와 도착지는 절대 포함하지 마. 출발지와 도착지는 별도 API 필드로 프론트엔드가 자동 입력한다.\n"
                        + "폼에는 동반 인원, 휠체어 사용 여부 등 메타데이터만 포함해.\n"
                        + "응답은 설명이나 Markdown 없이 완전한 JSON 배열 하나만 출력해. 각 항목의 label은 30자 이내로 하고, "
                        + "각 항목은 name, label, type, required를 포함하고, "
                        + "select/radio에만 options를 포함해. type은 text, number, select, radio 중 하나만 사용해.",
                targetExpln != null ? targetExpln : "없음", notice != null ? notice : "없음");
        return validateFormSchema(callOpenRouter(prompt, 32768));
    }

    private String validateFormSchema(String content) {
        try {
            JsonNode schema = objectMapper.readTree(content);
            if (schema == null || !schema.isArray()) {
                throw new IllegalArgumentException("폼 스키마는 JSON 배열이어야 합니다.");
            }
            for (JsonNode field : schema) {
                if (!field.isObject()
                    || !field.path("name").isTextual()
                    || !field.path("label").isTextual()
                    || !field.path("type").isTextual()
                        || !field.has("required")) {
                    throw new IllegalArgumentException("폼 항목의 필수 속성이 없습니다.");
                }
            }
            return objectMapper.writeValueAsString(schema);
        } catch (Exception exception) {
            log.warn("OpenRouter 폼 스키마가 유효한 JSON 배열이 아닙니다: {}", exception.getMessage());
            throw new IllegalStateException("유효하지 않은 폼 스키마 응답", exception);
        }
    }

    public boolean isValidFormSchema(String content) {
        try {
            validateFormSchema(content);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private String callOpenRouter(String prompt, int maxTokens) {
        long startedAt = System.nanoTime();
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "reasoning", Map.of("enabled", false),
                    "max_tokens", maxTokens);

            OpenRouterResponse response = openRouterWebClient.post()
                    .uri(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .retryWhen(Retry.backoff(1, Duration.ofSeconds(1))
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn(
                                "[OpenRouter 재시도] attempt={}, cause={}",
                                signal.totalRetries() + 1,
                                describeFailure(signal.failure()))))
                    .block();

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String content = response.choices().get(0).message().content();
                return content.replace("```json", "").replace("```", "").trim();
            }
            throw new IllegalStateException("OpenRouter 응답에 choices가 없습니다.");
        } catch (Exception exception) {
            WebClientResponseException responseException = findResponseException(exception);
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

            if (responseException != null) {
                log.error(
                        "[OpenRouter API 실패] model={}, maxTokens={}, promptLength={}, elapsedMs={}, "
                                + "status={}, headers={}, responseBody={}",
                        model,
                        maxTokens,
                        prompt.length(),
                        elapsedMs,
                        responseException.getStatusCode().value(),
                        extractDiagnosticHeaders(responseException),
                        responseException.getResponseBodyAsString(),
                        exception);
            } else {
                log.error(
                        "[OpenRouter 호출 실패] model={}, maxTokens={}, promptLength={}, elapsedMs={}, "
                                + "exceptionType={}, message={}",
                        model,
                        maxTokens,
                        prompt.length(),
                        elapsedMs,
                        exception.getClass().getName(),
                        exception.getMessage(),
                        exception);
            }
            throw internalServerError();
        }
    }

    private WebClientResponseException findResponseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebClientResponseException responseException) {
                return responseException;
            }
            current = current.getCause();
        }
        return null;
    }

    private Map<String, String> extractDiagnosticHeaders(WebClientResponseException exception) {
        Map<String, String> headers = new HashMap<>();
        exception.getHeaders().forEach((name, values) -> {
            String lowerCaseName = name.toLowerCase();
            if (lowerCaseName.contains("rate")
                    || lowerCaseName.contains("limit")
                    || lowerCaseName.equals("retry-after")
                    || lowerCaseName.equals("x-request-id")) {
                headers.put(name, String.join(",", values));
            }
        });
        return headers;
    }

    private String describeFailure(Throwable throwable) {
        WebClientResponseException responseException = findResponseException(throwable);
        if (responseException != null) {
            return "status=" + responseException.getStatusCode().value()
                    + ", body=" + responseException.getResponseBodyAsString();
        }
        return throwable == null ? "unknown" : throwable.getMessage();
    }

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException exception
                && (exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError());
    }

    private CustomException internalServerError() {
        return new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private record OpenRouterResponse(List<Choice> choices) {
        private record Choice(Message message) {}

        private record Message(String content) {}
    }
}