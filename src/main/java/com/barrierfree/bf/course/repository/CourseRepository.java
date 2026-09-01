package com.barrierfree.bf.course.repository;

import com.barrierfree.bf.course.entity.Course;
import com.barrierfree.bf.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 사용자의 코스 목록을 최신순으로 조회
    @EntityGraph(attributePaths = "places")
    List<Course> findAllByUserOrderByCreatedAtDesc(User user);

    // 단건 조회 시 코스 내 장소들까지 페치 조인(N+1 방지)하여 가져옴
    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.places WHERE c.id = :courseId AND c.user.id = :userId")
    Optional<Course> findByIdAndUserIdWithPlaces(@Param("courseId") Long courseId, @Param("userId") Long userId);

    // 본인의 코스가 맞는지 권한 체크용
    Optional<Course> findByIdAndUserId(Long id, Long userId);
}
