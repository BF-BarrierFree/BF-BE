package com.barrierfree.bf.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PolicyDocumentUpdateRequest(
    @NotBlank @Size(max = 100) String title, @NotBlank String content, LocalDateTime effectiveDate) {}
