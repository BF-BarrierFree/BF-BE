package com.barrierfree.bf.user.repository;

import com.barrierfree.bf.user.entity.UserTermAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

    // 특정 사용자의 전체 약관 동의 내역을 조회합니다.
    List<UserTermAgreement> findByUserId(Long userId);

    // 특정 사용자와 특정 약관의 동의 내역을 단건 조회합니다. (업데이트 및 중복 검증용)
    Optional<UserTermAgreement> findByUserIdAndTermId(Long userId, Long termId);
}
