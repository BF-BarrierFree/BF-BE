package com.barrierfree.bf.policy.repository;

import com.barrierfree.bf.policy.domain.PolicyCategory;
import com.barrierfree.bf.policy.entity.PolicyDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

  List<PolicyDocument> findAllByIsActiveTrueOrderByDisplayOrderAsc();

  Optional<PolicyDocument> findByCategoryAndIsActiveTrue(PolicyCategory category);

  Optional<PolicyDocument> findFirstByCategoryOrderByVersionDesc(PolicyCategory category);

  List<PolicyDocument> findAllByCategoryOrderByVersionDesc(PolicyCategory category);

  @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:category, 0))", nativeQuery = true)
  void lockCategory(@Param("category") String category);
}
