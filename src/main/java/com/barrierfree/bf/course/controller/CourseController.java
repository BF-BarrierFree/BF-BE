package com.barrierfree.bf.course.controller;

import com.barrierfree.bf.course.dto.CourseCreateRequest;
import com.barrierfree.bf.course.dto.CourseResponse;
import com.barrierfree.bf.course.dto.CourseUpdateRequest;
import com.barrierfree.bf.course.service.CourseService;
import com.barrierfree.bf.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "🚩 코스 (Course) API", description = "사용자 맞춤형 무장애 이동 코스 생성, 조회, 수정, 삭제 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(
        summary = "나만의 코스 직접 생성",
        description = "### 📌 기능 설명\n"
            + "사용자가 미리 저장한 장소들을 선택하여 새로운 이동 코스를 생성합니다.\n\n"
            + "### 💡 백엔드 자동 처리 로직\n"
            + "- 선택된 장소들의 **순서(sequence)** 를 보장합니다.\n"
            + "- OpenRouteService(ORS)를 호출하여 **장소 간 휠체어 이동 거리**를 자동 계산하여 저장합니다.\n"
            + "- 장소 카테고리에 맞춰 **무장애 동선 팁**을 자동 생성합니다."
    )
    @PostMapping
    public ApiResponse<CourseResponse> createCourse(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @Valid @RequestBody CourseCreateRequest request) {

        CourseResponse response = courseService.createCourse(userId, request);
        return ApiResponse.success(response, "코스가 성공적으로 생성되었습니다.");
    }

    @Operation(
        summary = "내 코스 목록 전체 조회",
        description = "### 📌 기능 설명\n"
            + "현재 로그인한 사용자가 지금까지 만든 모든 코스 목록을 조회합니다.\n"
            + "가장 최근에 생성된 코스부터 **최신순(내림차순)** 으로 정렬되어 반환됩니다."
    )
    @GetMapping
    public ApiResponse<List<CourseResponse>> getMyCourses(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        List<CourseResponse> response = courseService.getMyCourses(userId);
        return ApiResponse.success(response, "내 코스 목록을 조회했습니다.");
    }

    @Operation(
        summary = "특정 코스 상세 조회",
        description = "### 📌 기능 설명\n"
            + "코스 ID를 기반으로 특정 코스의 상세 정보를 조회합니다.\n"
            + "코스에 포함된 **장소 목록**, 장소 간 **이동 거리**, **동선 팁** 등이 포함됩니다.\n\n"
            + "⚠️ 본인이 생성한 코스만 조회할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코스를 찾을 수 없거나 접근 권한이 없음 (C001)", content = @Content)
    })
    @GetMapping("/{courseId}")
    public ApiResponse<CourseResponse> getCourseDetail(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @Parameter(description = "조회할 코스의 ID", example = "1") @PathVariable Long courseId) {

        CourseResponse response = courseService.getCourseDetail(userId, courseId);
        return ApiResponse.success(response, "코스 상세 정보를 조회했습니다.");
    }

    @Operation(
        summary = "코스 정보 및 장소 구성 수정",
        description = "### 📌 기능 설명\n"
            + "기존에 만들어둔 코스의 **제목**이나 포함된 **장소 목록 및 순서**를 전면 수정합니다.\n\n"
            + "### 💡 참고 사항\n"
            + "- 수정 시 장소 리스트가 통째로 교체되므로, **유지할 장소와 추가할 장소의 ID를 모두 순서대로 배열**에 담아 보내야 합니다.\n"
            + "- 장소 구성이 바뀌면 백엔드에서 **거리를 다시 계산**합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코스를 찾을 수 없거나 접근 권한이 없음 (C001)", content = @Content)
    })
    @PutMapping("/{courseId}")
    public ApiResponse<CourseResponse> updateCourse(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @Parameter(description = "수정할 코스의 ID", example = "1") @PathVariable Long courseId,
        @Valid @RequestBody CourseUpdateRequest request) {

        CourseResponse response = courseService.updateCourse(userId, courseId, request);
        return ApiResponse.success(response, "코스가 성공적으로 수정되었습니다.");
    }

    @Operation(
        summary = "코스 삭제",
        description = "### 📌 기능 설명\n"
            + "특정 코스를 완전히 삭제합니다.\n"
            + "코스를 삭제해도 원본으로 사용했던 '저장된 장소(SavedPlace)' 데이터는 지워지지 않습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코스를 찾을 수 없거나 접근 권한이 없음 (C001)", content = @Content)
    })
    @DeleteMapping("/{courseId}")
    public ApiResponse<?> deleteCourse(
        @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
        @Parameter(description = "삭제할 코스의 ID", example = "1") @PathVariable Long courseId) {

        courseService.deleteCourse(userId, courseId);
        return ApiResponse.successWithNoContent();
    }
}
