package com.barrierfree.bf.policy.dto;

import com.barrierfree.bf.policy.domain.PolicyCategory;

public record PolicyCategoryResponse(PolicyCategory category, String label, Integer displayOrder) {

  public static PolicyCategoryResponse from(PolicyCategory category) {
    return new PolicyCategoryResponse(category, category.getLabel(), category.getDisplayOrder());
  }
}
