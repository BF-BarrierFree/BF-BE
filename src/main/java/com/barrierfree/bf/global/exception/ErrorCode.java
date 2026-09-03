package com.barrierfree.bf.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- 공통 에러 (Global) ---
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G002", "서버 내부 오류가 발생했습니다."),

    // --- 유저 및 온보딩 관련 에러 (User) ---
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자 정보를 찾을 수 없습니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "U002", "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "U003", "닉네임 형식이 올바르지 않습니다. (15자 이내)"),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "U004", "필수 약관에 모두 동의해야 합니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "U005", "이미 온보딩이 완료된 사용자입니다."),

    // --- 약관 관련 에러 (Term) ---
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "해당 약관을 찾을 수 없습니다."),
    REQUIRED_TERM_CANCELLATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T002", "필수 약관은 동의를 취소할 수 없습니다."),

    // --- 공지사항 관련 에러 (Notice) ---
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "공지사항을 찾을 수 없습니다."),

    // --- 인증 관련 에러 (Auth/Kakao) ---
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A001", "카카오 로그인 처리에 실패했습니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "A002", "카카오 사용자 정보를 가져오지 못했습니다."),
    KAKAO_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A003", "카카오 인증 서비스를 현재 사용할 수 없습니다."),

    // --- 무장애 시설 관련 에러 (Facility) ---
    FACILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "해당 시설을 찾을 수 없습니다."),
    SAVED_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "저장된 장소를 찾을 수 없습니다."),

    // --- 이미지 관련 에러 (Image) ---
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "I001", "지원하지 않는 이미지 형식입니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I002", "이미지 업로드에 실패했습니다."),

    // --- 외부 API 관련 에러 (Map) ---
    GOOGLE_MAP_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "M001", "구글맵 API 호출에 실패했습니다."),
    GOOGLE_MAP_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "M002", "구글맵 API 응답이 지연되고 있습니다."),

    // --- 길찾기 및 경로 탐색 관련 에러 (Route) ---
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "요청하신 조건으로 이동 가능한 휠체어 경로를 찾을 수 없습니다."),
    INVALID_ROUTE_COORDINATES(HttpStatus.BAD_REQUEST, "R002", "출발지 또는 도착지의 좌표가 올바르지 않습니다. (예: 바다 한가운데)"),
    EXTERNAL_ROUTING_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R003", "길찾기 서버와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."),
    ODSAY_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R004", "대중교통 길찾기 정보를 불러오는데 실패했습니다."),
    TAGO_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R005", "공공데이터(버스 정보)를 불러오는데 실패했습니다."),
    KAKAO_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R006", "카카오 모빌리티 길찾기 정보를 불러오는데 실패했습니다."),
    KAKAO_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "R007", "카카오 모빌리티 API 응답이 지연되고 있습니다."),

    // --- 코스 관련 에러 (Course) ---
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "요청하신 코스를 찾을 수 없습니다."),
    COURSE_UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "C002", "해당 코스에 대한 접근 권한이 없습니다."),
    COURSE_PLACE_EMPTY(HttpStatus.BAD_REQUEST, "C003", "코스에는 최소 한 개 이상의 장소가 포함되어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
