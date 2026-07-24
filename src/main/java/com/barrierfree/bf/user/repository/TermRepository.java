package com.barrierfree.bf.user.repository;

import com.barrierfree.bf.user.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TermRepository extends JpaRepository<Term, Long> {

    // 현재 활성화(사용 중)인 약관 목록만 모두 가져옵니다.
    List<Term> findByIsActiveTrue();
}
