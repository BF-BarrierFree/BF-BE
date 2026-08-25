package com.barrierfree.bf.review.repository;

import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.review.dto.FacilityCountDto;
import com.barrierfree.bf.review.entity.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  /**
   * 1. [하이브리드 - RDBMS] 동적 필터링 조회 (Specification 대체) - 컬렉션(mobilities, facilities)이 비어있을 때 IN 절에서
   * 발생하는 SQL 문법 에러를 막기 위해 Service 계층에서 hasMobilities, hasFacilities 플래그를 넘겨받아 쿼리를 안전하게
   * 무시(Bypass)합니다.
   */
  @Query(
      "SELECT DISTINCT r FROM Review r "
          + "LEFT JOIN r.mobilities m "
          + "LEFT JOIN r.facilities f "
          + "WHERE r.isDeleted = false "
          + "AND (:placeId IS NULL OR r.placeId = :placeId) "
          + "AND (:category IS NULL OR r.category = :category) "
          + "AND (:minRating IS NULL OR r.rating >= :minRating) "
          + "AND (:hasMobilities = false OR m IN :mobilities) "
          + "AND (:hasFacilities = false OR f IN :facilities)")
  Page<Review> findFilteredReviews(
      @Param("placeId") String placeId,
      @Param("category") PlaceCategory category,
      @Param("minRating") Integer minRating,
      @Param("mobilities") List<MobilityType> mobilities,
      @Param("hasMobilities") boolean hasMobilities,
      @Param("facilities") List<FacilityType> facilities,
      @Param("hasFacilities") boolean hasFacilities,
      Pageable pageable);

  /**
   * 2. [하이브리드 - 통계] 특정 장소의 접근성 시설 카운트 - DB 단에서 GROUP BY와 COUNT 연산을 끝내고 바로 DTO로 매핑하여 응답 속도를 극대화합니다.
   */
  @Query(
      "SELECT new com.barrierfree.bf.review.dto.FacilityCountDto(f, COUNT(r)) "
          + "FROM Review r JOIN r.facilities f "
          + "WHERE r.placeId = :placeId AND r.isDeleted = false "
          + "GROUP BY f")
  List<FacilityCountDto> countFacilitiesByPlaceId(@Param("placeId") String placeId);

  /**
   * 3. [하이브리드 - pgvector] 자연어 기반 유사도 검색 (Native Query) - Gemini 임베딩 모델에 가장 적합한 코사인 유사도 연산자(<=>)를
   * 사용합니다. - JPA Native Query에서 페이징(Pageable)을 작동시키기 위해 countQuery를 직접 명시했습니다. - 안전한 타입 캐스팅을 위해
   * Service에서 float[]를 String(예: "[0.1, 0.2]")으로 변환하여 주입합니다.
   */
  @Query(
      value =
          "SELECT * FROM reviews r WHERE r.is_deleted = false ORDER BY r.embedding <=> CAST(:embedding AS vector)",
      countQuery = "SELECT count(*) FROM reviews r WHERE r.is_deleted = false",
      nativeQuery = true)
  Page<Review> findSimilarReviewsByEmbedding(
      @Param("embedding") String embeddingString, Pageable pageable);
}
