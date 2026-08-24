package com.barrierfree.bf.global.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class JinaEmbeddingResponse {
  private String model;
  private List<EmbeddingData> data;

  @Getter
  public static class EmbeddingData {
    private int index;
    private float[] embedding;
  }
}
