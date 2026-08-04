package com.barrierfree.bf.global.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JinaEmbeddingRequest {
    private String model;
    private String task;
    private boolean normalized; // 코사인 유사도 검색을 위해 true 설정 필수
    private List<String> input;
}