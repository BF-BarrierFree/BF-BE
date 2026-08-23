package com.barrierfree.bf.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
    @NotBlank @Size(max = 20) String category,
    @NotBlank @Size(max = 100) String title,
    @NotBlank String content) {}
