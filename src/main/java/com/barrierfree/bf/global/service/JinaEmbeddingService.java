package com.barrierfree.bf.global.service;

import com.barrierfree.bf.global.dto.JinaEmbeddingRequest;
import com.barrierfree.bf.global.dto.JinaEmbeddingResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JinaEmbeddingService {

  private final WebClient webClient;

  @Value("${jina.api-key}")
  private String apiKey;

  private static final String API_URL = "https://api.jina.ai/v1/embeddings";
  private static final String MODEL_NAME = "jina-embeddings-v5-text-small";
  public static final int DIMENSION_SIZE = 1024;

  /**
   * @param text 임베딩할 텍스트
   * @param task 용도 구분 (저장용: "retrieval.passage", 검색용: "retrieval.query")
   */
  public float[] getEmbedding(String text, String task) {
    if (text == null || text.trim().isEmpty()) {
      return new float[DIMENSION_SIZE];
    }

    try {
      JinaEmbeddingRequest requestBody =
          JinaEmbeddingRequest.builder()
              .model(MODEL_NAME)
              .task(task)
              .normalized(true) // L2 정규화 (필수)
              .input(List.of(text))
              .build();

      JinaEmbeddingResponse response =
          webClient
              .post()
              .uri(API_URL)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(requestBody)
              .retrieve()
              .bodyToMono(JinaEmbeddingResponse.class)
              .block();

      if (response != null && response.getData() != null && !response.getData().isEmpty()) {
        return response.getData().get(0).getEmbedding();
      }

      log.warn("Jina API 응답에 임베딩 데이터가 없습니다. 빈 벡터를 반환합니다.");
      return new float[DIMENSION_SIZE];

    } catch (WebClientResponseException e) {
      log.error(
          "🚨 [Jina API 에러] Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
      return new float[DIMENSION_SIZE];
    } catch (Exception e) {
      log.error("Jina 임베딩 생성 실패: {}", e.getMessage(), e);
      return new float[DIMENSION_SIZE];
    }
  }
}
