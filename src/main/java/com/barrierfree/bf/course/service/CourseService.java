package com.barrierfree.bf.course.service;

import com.barrierfree.bf.course.dto.CourseCreateRequest;
import com.barrierfree.bf.course.dto.CourseResponse;
import com.barrierfree.bf.course.dto.CourseUpdateRequest;
import com.barrierfree.bf.course.entity.Course;
import com.barrierfree.bf.course.entity.CoursePlace;
import com.barrierfree.bf.course.repository.CourseRepository;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.entity.SavedPlace;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final UserRepository userRepository;
    private final OrsRouteService orsRouteService;

    /**
     * 사용자가 선택한 장소들을 기반으로 새로운 코스를 생성합니다.
     */
    @Transactional
    public CourseResponse createCourse(Long userId, CourseCreateRequest request) {
        User user = getUser(userId);

        // 1. 코스 엔티티 생성 (직접 생성이므로 isAiGenerated = false)
        Course course = Course.builder()
            .user(user)
            .title(request.title())
            .isAiGenerated(false)
            .build();

        // 2. 전달받은 장소 ID 리스트를 순회하며 CoursePlace 스냅샷 생성 및 거리 계산
        buildCoursePlaces(course, request.savedPlaceIds());

        // 3. 저장 및 반환
        Course savedCourse = courseRepository.save(course);
        return CourseResponse.from(savedCourse);
    }

    /**
     * 유저의 모든 코스 목록을 최신순으로 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getMyCourses(Long userId) {
        User user = getUser(userId);
        List<Course> courses = courseRepository.findAllByUserOrderByCreatedAtDesc(user);

        return courses.stream()
            .map(CourseResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * 특정 코스의 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseDetail(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserIdWithPlaces(courseId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        return CourseResponse.from(course);
    }

    /**
     * 코스의 제목 및 장소 구성(순서 포함)을 수정합니다.
     */
    @Transactional
    public CourseResponse updateCourse(Long userId, Long courseId, CourseUpdateRequest request) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        // 제목 업데이트
        course.updateTitle(request.title());

        // 기존 장소들 제거 후 새롭게 구성 (JPA orphanRemoval에 의해 삭제됨)
        course.getPlaces().clear();
        buildCoursePlaces(course, request.savedPlaceIds());

        return CourseResponse.from(course);
    }

    /**
     * 코스를 삭제합니다. (하위 장소들도 연쇄 삭제됨)
     */
    @Transactional
    public void deleteCourse(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        courseRepository.delete(course);
    }

    // --- Helper Methods ---

    private User getUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 장소 ID 목록을 순회하며 코스에 들어갈 장소(CoursePlace)를 빌드하고 거리를 계산합니다.
     */
    private void buildCoursePlaces(Course course, List<Long> savedPlaceIds) {
        if (savedPlaceIds == null || savedPlaceIds.isEmpty()) {
            throw new CustomException(ErrorCode.COURSE_PLACE_EMPTY);
        }

        for (int i = 0; i < savedPlaceIds.size(); i++) {
            Long placeId = savedPlaceIds.get(i);
            SavedPlace savedPlace = savedPlaceRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.SAVED_PLACE_NOT_FOUND));

            String distanceToNext = null;

            // 마지막 장소가 아니면 다음 장소까지의 휠체어 이동 거리를 계산
            if (i < savedPlaceIds.size() - 1) {
                Long nextPlaceId = savedPlaceIds.get(i + 1);
                SavedPlace nextPlace = savedPlaceRepository.findById(nextPlaceId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SAVED_PLACE_NOT_FOUND));

                distanceToNext = calculateDistanceToNext(savedPlace, nextPlace);
            }

            // 스냅샷 엔티티 생성
            CoursePlace coursePlace = CoursePlace.builder()
                .sequence(i)
                .originalPlaceId(savedPlace.getPlaceId()) // 구글 맵 등의 원본 placeId
                .name(savedPlace.getName())
                .category(savedPlace.getCategory())
                .address(savedPlace.getAddress())
                .latitude(savedPlace.getLatitude())
                .longitude(savedPlace.getLongitude())
                .photoUrl(savedPlace.getPhotoUrl())
                .distanceToNext(distanceToNext)
                .movingTip(generateMovingTip(savedPlace.getCategory())) // 카테고리 기반 팁
                .build();

            course.addPlace(coursePlace);
        }
    }

    /**
     * 두 장소 간의 휠체어 길찾기 결과를 요청하고, 미터(m) 단위를 읽기 좋은 포맷으로 변환합니다.
     */
    private String calculateDistanceToNext(SavedPlace current, SavedPlace next) {
        try {
            WheelchairRouteResponse routeResponse = orsRouteService.getWheelchairRoute(
                current.getLongitude(), current.getLatitude(),
                next.getLongitude(), next.getLatitude()
            );

            double meters = routeResponse.totalDistanceMeter();
            return formatDistance(meters);

        } catch (CustomException e) {
            log.warn("경로를 찾을 수 없거나 에러가 발생했습니다. (출발지: {}, 도착지: {})", current.getName(), next.getName());
            return "경로 없음"; // 프론트엔드에서 예외 처리할 수 있도록 텍스트 반환
        } catch (Exception e) {
            log.error("경로 탐색 중 예상치 못한 에러 발생", e);
            return "거리 계산 실패";
        }
    }

    /**
     * 거리를 UI 친화적인 포맷(예: "1.3km", "500m")으로 변환합니다.
     */
    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format("%.0fm", meters); // 예: 500m
        } else {
            return String.format("%.1fkm", meters / 1000.0); // 예: 1.3km
        }
    }

    /**
     * 장소 카테고리에 맞춰 동선 팁을 생성합니다. (기획에 맞춰 추후 내용 고도화 가능)
     */
    private String generateMovingTip(PlaceCategory category) {
        if (category == null) return "접근로 단차에 유의하며 이동하세요.";

        return switch (category) {
            case FOOD_CAFE -> "출입구 턱이나 휠체어 진입 가능 여부를 매장에 미리 확인하시면 좋습니다.";
            case TOUR_CULTURE -> "관광지 내 휠체어용 완만한 경사로나 전용 산책로를 이용해 보세요.";
            case PARK_TRAIL -> "포장된 산책로를 이용하시고, 경사 구간에서는 주의해 주세요.";
            case LODGING -> "장애인 전용 객실이나 엘리베이터 접근성을 데스크에 문의하세요.";
            case TRANSPORTATION -> "엘리베이터 위치나 교통약자 승하차 지원 서비스를 미리 확인하세요.";
            case PUBLIC_FACILITY -> "엘리베이터가 잘 갖춰져 있으며, 1층 중심 동선을 추천합니다.";
            case ETC -> "완만한 경사로를 이용하고, 필요한 경우 내부 안내 데스크에 문의하세요.";
        };
    }
}
