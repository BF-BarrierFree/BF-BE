package com.barrierfree.bf.course.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "courses",
    indexes = {@Index(name = "idx_course_user_id", columnList = "user_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean isAiGenerated;

    // 코스가 삭제되면 하위 장소들도 삭제되도록 Cascade 적용
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC") // 조회 시 항상 sequence(순서) 기준으로 정렬되도록 보장
    private List<CoursePlace> places = new ArrayList<>();

    @Builder
    public Course(User user, String title, boolean isAiGenerated) {
        this.user = user;
        this.title = title;
        this.isAiGenerated = isAiGenerated;
    }

    /**
     * 코스의 제목을 수정합니다.
     */
    public void updateTitle(String title) {
        this.title = title;
    }

    /**
     * 연관관계 편의 메서드: 코스에 장소를 추가합니다.
     */
    public void addPlace(CoursePlace place) {
        this.places.add(place);
        place.setCourse(this);
    }
}
