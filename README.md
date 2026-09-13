# BF-BE

Barrier Free 서비스의 Spring Boot 백엔드입니다. 장소 검색, 무장애 정보, 리뷰, 경로 탐색, 코스 생성, 장애인 콜택시, 공지/정책 문서, 사용자 인증 및 설정 API를 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 4.0.6
- Spring Security, JWT
- Spring Data JPA, PostgreSQL
- Flyway
- WebClient
- Springdoc OpenAPI / Swagger UI
- Cloudflare R2 compatible S3
- Gradle
- Spotless / googleJavaFormat

## 주요 기능

- 카카오 로그인 및 JWT 기반 인증
- 사용자 온보딩, 프로필, 선호 설정, 약관 동의 관리
- Google Places 기반 장소 자동완성, 검색, 상세 조회
- 관광공사/공공데이터 기반 무장애 장소 정보 보강
- 장소 저장 목록 및 검색 기록 관리
- 리뷰 작성, 이미지 업로드, 도움돼요, 시설 통계
- 도보, 휠체어, 차량, 대중교통 경로 탐색
- 장애인 콜택시 센터 조회, 요금 산출, 예약
- 사용자 코스 CRUD 및 AI 코스 추천
- 공지사항, 정책 문서, 1:1 문의 관리

## 프로젝트 구조

```text
src/main/java/com/barrierfree/bf
├── auth       # 카카오 로그인, 토큰 발급/로그아웃
├── course     # 사용자 코스, AI 코스 추천
├── global     # 공통 응답, 예외, 인증, 이미지, 공통 설정
├── inquiry    # 1:1 문의
├── mobility   # 교통약자 이동지원 외부 API 테스트
├── notice     # 공지사항
├── place      # 장소 검색, 저장 장소, 무장애 정보
├── policy     # 정책 문서
├── review     # 장소 리뷰
├── route      # 도보/휠체어/차량/대중교통 경로
├── taxi       # 장애인 콜택시
└── user       # 사용자, 약관, 선호 설정
```

## 실행 전 준비

### 필수 환경변수

`application.yaml`은 환경변수를 참조합니다. 로컬에서는 IntelliJ Run Configuration의 Environment variables에 넣거나, 실행 전에 OS 환경변수로 설정합니다.


`.env` 파일이 있어도 Spring Boot가 모든 값을 자동으로 읽는 구조는 아닙니다. 로컬에서 값이 주입되지 않으면 IntelliJ 실행 설정이나 쉘 환경변수에 직접 넣어 실행하세요.

## 테스트 및 포맷

전체 테스트:

```powershell
.\gradlew.bat test
```

코드 포맷 적용:

```powershell
.\gradlew.bat spotlessApply
```

## 개발 메모

- Java 코드는 Spotless의 `googleJavaFormat`을 기준으로 정리합니다.
- DB 마이그레이션은 `src/main/resources/db/migration` 아래 Flyway 파일로 관리합니다.
- 로컬 `application.yaml`의 `spring.jpa.hibernate.ddl-auto`는 현재 `update`입니다. 운영에서는 `validate` 또는 마이그레이션 중심 운영을 권장합니다.
- 외부 API 키와 클라우드 스토리지 키는 커밋하지 않습니다.
