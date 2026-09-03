package com.barrierfree.bf.user.repository;

import com.barrierfree.bf.user.entity.Term;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermRepository extends JpaRepository<Term, Long> {

    // 현재 활성화(사용 중)인 약관 목록만 모두 가져옵니다.
    List<Term> findAllByIsActiveTrue();

    // 현재 활성화(사용 중)인 필수 약관 목록만 모두 가져옵니다.
    List<Term> findAllByIsRequiredTrueAndIsActiveTrue();

    Optional<Term> findFirstByTermKeyOrderByVersionDesc(String termKey);

    Optional<Term> findByTermKeyAndIsActiveTrue(String termKey);

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtextextended(:termKey, 0))",
        nativeQuery = true)
    void lockTermKey(@Param("termKey") String termKey);
}
