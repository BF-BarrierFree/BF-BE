package com.barrierfree.bf.review.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.global.enums.FacilityType;
import com.barrierfree.bf.global.enums.MobilityType;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 무장애 지도의 장소 리뷰를 관리하는 Entity
 * 하이브리드 검색을 위해 일반 텍스트 정보와 임베딩(Vector) 데이터를 함께 저장합니다.
 */
@Entity
// placeId로 특정 장소의 리뷰를 조회하는 쿼리가 압도적으로 많을 것이므로, 성능 최적화를 위해 인덱스를 걸어줍니다.
@Table(name = "reviews", indexes = {@Index(name = "idx_review_place_id", columnList = "placeId")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리뷰 작성자 (N:1 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 구글 맵스 API에서 제공하는 장소 고유 ID (별도의 Place 엔티티 없이 외래키처럼 사용)
    @Column(nullable = false, length = 100)
    private String placeId;

    // 리뷰 내용 본문
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 장소명 (프론트에서 전달받아 임베딩 및 글로벌 조회 시 사용)
    @Column(nullable = false)
    private String placeName;

    // 장소 카테고리 (글로벌 필터링 용도)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceCategory category;

    // 별점 (1~5)
    @Column(nullable = false)
    private Integer rating;

    // === Vector DB (pgvector) 매핑 ===
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    /*
     * [수정] N+1 및 MultipleBagFetchException 방지를 위한 BatchSize 추가!
     */
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_mobilities", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "mobility_type")
    private List<MobilityType> mobilities = new ArrayList<>();

    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_facilities", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type")
    private List<FacilityType> facilities = new ArrayList<>();

    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @Builder
    public Review(User user, String placeId, String content, String placeName, PlaceCategory category, Integer rating, float[] embedding, 
                  List<MobilityType> mobilities, List<FacilityType> facilities, List<String> imageUrls) {
        this.user = user;
        this.placeId = placeId;
        this.content = content;
        this.placeName = placeName;
        this.category = category != null ? category : PlaceCategory.ETC;
        this.rating = rating != null ? rating : 5;
        this.embedding = embedding;
        this.mobilities = mobilities != null ? mobilities : new ArrayList<>();
        this.facilities = facilities != null ? facilities : new ArrayList<>();
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    /**
     * 리뷰 삭제 (Soft Delete)
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}