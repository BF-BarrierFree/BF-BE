package com.barrierfree.bf.user.repository;

import com.barrierfree.bf.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 고유 ID로 탈퇴하지 않은 유저를 찾습니다. (로그인 시 사용)
    Optional<User> findBySocialIdAndIsDeletedFalse(String socialId);

    // 유저 PK(Id)로 탈퇴하지 않은 유저를 찾습니다. (토큰 검증 등에서 사용)
    Optional<User> findByIdAndIsDeletedFalse(Long id);

    // 닉네임 중복 검사를 위한 메서드 (온보딩 및 닉네임 변경 시 사용)
    boolean existsByNickname(String nickname);
}
