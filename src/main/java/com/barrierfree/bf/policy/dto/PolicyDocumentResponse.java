package com.barrierfree.bf.policy.dto;

import com.barrierfree.bf.policy.domain.PolicyCategory;
import com.barrierfree.bf.policy.entity.PolicyDocument;
import java.time.LocalDateTime;

public record PolicyDocumentResponse(
    Long id,
    PolicyCategory category,
    String categoryLabel,
    String title,
    String content,
    Integer version,
    Integer displayOrder,
    LocalDateTime effectiveDate,
    LocalDateTime createdAt) {

  public static PolicyDocumentResponse from(PolicyDocument policyDocument) {
    PolicyCategory category = policyDocument.getCategory();
    return new PolicyDocumentResponse(
        policyDocument.getId(),
        category,
        category.getLabel(),
        policyDocument.getTitle(),
        policyDocument.getContent(),
        policyDocument.getVersion(),
        policyDocument.getDisplayOrder(),
        policyDocument.getEffectiveDate(),
        policyDocument.getCreatedAt());
  }
}
