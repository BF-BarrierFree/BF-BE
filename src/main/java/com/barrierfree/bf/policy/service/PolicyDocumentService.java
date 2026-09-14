package com.barrierfree.bf.policy.service;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.policy.domain.PolicyCategory;
import com.barrierfree.bf.policy.dto.PolicyCategoryResponse;
import com.barrierfree.bf.policy.dto.PolicyDocumentResponse;
import com.barrierfree.bf.policy.dto.PolicyDocumentUpdateRequest;
import com.barrierfree.bf.policy.entity.PolicyDocument;
import com.barrierfree.bf.policy.repository.PolicyDocumentRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyDocumentService {

  private final PolicyDocumentRepository policyDocumentRepository;

  public List<PolicyCategoryResponse> getCategories() {
    return Arrays.stream(PolicyCategory.values())
        .sorted(Comparator.comparingInt(PolicyCategory::getDisplayOrder))
        .map(PolicyCategoryResponse::from)
        .toList();
  }

  public List<PolicyDocumentResponse> getActivePolicies() {
    return policyDocumentRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc().stream()
        .map(PolicyDocumentResponse::from)
        .toList();
  }

  public PolicyDocumentResponse getActivePolicy(String categoryValue) {
    PolicyCategory category = PolicyCategory.from(categoryValue);
    PolicyDocument policyDocument =
        policyDocumentRepository
            .findByCategoryAndIsActiveTrue(category)
            .orElseThrow(() -> new CustomException(ErrorCode.TERM_NOT_FOUND));
    return PolicyDocumentResponse.from(policyDocument);
  }

  public List<PolicyDocumentResponse> getPolicyVersions(String categoryValue) {
    PolicyCategory category = PolicyCategory.from(categoryValue);
    return policyDocumentRepository.findAllByCategoryOrderByVersionDesc(category).stream()
        .map(PolicyDocumentResponse::from)
        .toList();
  }

  @Transactional
  public PolicyDocumentResponse publishPolicy(
      String categoryValue, PolicyDocumentUpdateRequest request) {
    PolicyCategory category = PolicyCategory.from(categoryValue);
    policyDocumentRepository.lockCategory(category.name());

    PolicyDocument latestRevision =
        policyDocumentRepository.findFirstByCategoryOrderByVersionDesc(category).orElse(null);
    int nextVersion = latestRevision == null ? 1 : latestRevision.getVersion() + 1;
    policyDocumentRepository
        .findByCategoryAndIsActiveTrue(category)
        .ifPresent(PolicyDocument::deactivate);

    PolicyDocument policyDocument =
        PolicyDocument.builder()
            .category(category)
            .title(request.title().trim())
            .content(request.content().trim())
            .version(nextVersion)
            .displayOrder(category.getDisplayOrder())
            .effectiveDate(request.effectiveDate())
            .isActive(true)
            .build();

    return PolicyDocumentResponse.from(policyDocumentRepository.save(policyDocument));
  }
}
