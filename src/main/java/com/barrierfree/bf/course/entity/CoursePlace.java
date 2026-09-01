package com.barrierfree.bf.course.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.place.domain.PlaceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(nullable = false)
  private Integer sequence; // 코스 내 장소 순서 (0, 1, 2...)

  // 원본 장소가 삭제되어도 코스는 유지되도록 스냅샷 데이터를 저장합니다.
  @Column(name = "original_place_id", length = 150)
  private String originalPlaceId;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private PlaceCategory category;

  @Column(length = 500)
  private String address;

  private Double latitude;

  private Double longitude;

  @Column(length = 1000)
  private String photoUrl;

  // 다음 장소까지의 거리 (예: "1.3km", "500m" 또는 미터 단위의 정수. 유연성을 위해 문자열 허용)
  @Column(length = 50)
  private String distanceToNext;

  // 장소 간 이동 시 무장애 동선 팁 (예: "완만한 경사로 이용, 내부 안내 데스크 문의")
  @Column(length = 500)
  private String movingTip;

  @Builder
  public CoursePlace(
      Integer sequence,
      String originalPlaceId,
      String name,
      PlaceCategory category,
      String address,
      Double latitude,
      Double longitude,
      String photoUrl,
      String distanceToNext,
      String movingTip) {
    this.sequence = sequence;
    this.originalPlaceId = originalPlaceId;
    this.name = name;
    this.category = category == null ? PlaceCategory.ETC : category;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.photoUrl = photoUrl;
    this.distanceToNext = distanceToNext;
    this.movingTip = movingTip;
  }

  /** 연관관계 편의 메서드용 setter (Course Entity에서 호출) */
  protected void setCourse(Course course) {
    this.course = course;
  }
}
