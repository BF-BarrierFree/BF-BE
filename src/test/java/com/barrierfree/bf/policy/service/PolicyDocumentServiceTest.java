package com.barrierfree.bf.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.policy.domain.PolicyCategory;
import com.barrierfree.bf.policy.dto.PolicyDocumentUpdateRequest;
import com.barrierfree.bf.policy.entity.PolicyDocument;
import com.barrierfree.bf.policy.repository.PolicyDocumentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyDocumentServiceTest {

  private PolicyDocumentRepository policyDocumentRepository;
  private PolicyDocumentService policyDocumentService;

  @BeforeEach
  void setUp() {
    policyDocumentRepository = mock(PolicyDocumentRepository.class);
    policyDocumentService = new PolicyDocumentService(policyDocumentRepository);
  }

  @Test
  void returnsPolicyCategoriesInDisplayOrder() {
    var categories = policyDocumentService.getCategories();

    assertThat(categories)
        .extracting(category -> category.category())
        .containsExactly(
            PolicyCategory.TERMS_OF_SERVICE,
            PolicyCategory.PRIVACY_POLICY,
            PolicyCategory.LOCATION_BASED_SERVICE_TERMS,
            PolicyCategory.ACCESSIBILITY_INFO_DISCLAIMER,
            PolicyCategory.COMMUNITY_POLICY);
  }

  @Test
  void publishesFirstPolicyVersion() {
    when(policyDocumentRepository.findFirstByCategoryOrderByVersionDesc(
            PolicyCategory.PRIVACY_POLICY))
        .thenReturn(Optional.empty());
    when(policyDocumentRepository.save(any(PolicyDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        policyDocumentService.publishPolicy(
            "PRIVACY_POLICY", new PolicyDocumentUpdateRequest(" Privacy ", " Content ", null));

    verify(policyDocumentRepository).lockCategory("PRIVACY_POLICY");
    assertThat(response.category()).isEqualTo(PolicyCategory.PRIVACY_POLICY);
    assertThat(response.title()).isEqualTo("Privacy");
    assertThat(response.content()).isEqualTo("Content");
    assertThat(response.version()).isEqualTo(1);
  }

  @Test
  void publishesNextVersionAndDeactivatesCurrentPolicy() {
    PolicyDocument currentPolicy =
        PolicyDocument.builder()
            .category(PolicyCategory.PRIVACY_POLICY)
            .title("Old privacy")
            .content("Old content")
            .version(2)
            .isActive(true)
            .build();

    when(policyDocumentRepository.findFirstByCategoryOrderByVersionDesc(
            PolicyCategory.PRIVACY_POLICY))
        .thenReturn(Optional.of(currentPolicy));
    when(policyDocumentRepository.findByCategoryAndIsActiveTrue(PolicyCategory.PRIVACY_POLICY))
        .thenReturn(Optional.of(currentPolicy));
    when(policyDocumentRepository.save(any(PolicyDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        policyDocumentService.publishPolicy(
            "PRIVACY_POLICY", new PolicyDocumentUpdateRequest("New privacy", "New content", null));

    assertThat(currentPolicy.isActive()).isFalse();
    assertThat(response.version()).isEqualTo(3);
  }
}
