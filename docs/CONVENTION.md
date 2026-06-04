# BF-BE 프로젝트 컨벤션 가이드

이 문서는 Barrier-Free(BF) 백엔드 프로젝트의 원활한 협업을 위한 규칙을 정의합니다.

## 1. 브랜치 전략 (GitHub Flow)
작업은 항상 `main` 브랜치에서 파생된 개별 브랜치에서 진행하며, 작업 완료 후 PR(Pull Request)을 통해 머지합니다.

* **형식**: `타입/#이슈번호-작업내용` (작업내용은 케밥 케이스 사용)
* **예시**:
  * 기능 추가: `feature/#12-google-map-api`
  * 버그 수정: `bugfix/#15-login-timeout`
  * 긴급 수정: `hotfix/#20-db-connection-error`
  * 설정/문서: `chore/#5-add-redis-config`

## 2. 코드 및 네이밍 컨벤션
일관된 코드 스타일을 유지하여 가독성을 높입니다.

* **클래스/인터페이스**: `PascalCase` (예: `FacilityController`)
* **메서드/변수**: `camelCase` (예: `getAccessibilityData`)
* **상수**: `UPPER_SNAKE_CASE` (예: `MAX_UPLOAD_SIZE`)
* **DB 테이블/컬럼**: `snake_case` (예: `facility_info`)

### 2.1 계층별 네이밍 (Spring Boot)
* **Controller**: `~Controller`
* **Service**: `~Service`
* **Repository**: `~Repository`
* **DTO**: 요청은 `~Request`, 응답은 `~Response` 사용 (예: `FacilitySearchRequest`)

### 2.2 코드 포맷팅 세부 규칙 (매우 중요)
1. **와일드카드 임포트(`import *`) 절대 금지**
   - 클래스 이름이 겹치거나 어떤 패키지에서 가져왔는지 추적이 어려워집니다.
2. **들여쓰기(Indent)는 무조건 Space 4칸**
   - Java 코드는 Tab 대신 Space 4칸을 사용합니다. (`.editorconfig` 및 Spotless 적용됨)
3. **중괄호 `{ }` 위치 (K&R 스타일)**
   - 클래스와 메서드의 여는 중괄호 `{`는 같은 줄에 작성합니다.
   - 제어문(`if`, `for` 등)의 코드가 한 줄이더라도 반드시 중괄호를 사용합니다.

## 3. 커밋 메시지 컨벤션 (Conventional Commits)
커밋 메시지 작성 시 `.gitmessage.txt` 템플릿을 활용하며, 아래 태그를 준수합니다.

* `feat` : 새로운 기능 추가
* `fix` : 버그 수정
* `docs` : 문서 수정
* `style` : 코드 포맷팅 (로직 변경 없음)
* `refactor` : 코드 리팩토링 (기능 변화 없음)
* `test` : 테스트 코드 추가/수정
* `chore` : 빌드 설정, 의존성 변경 등

## 4. API 응답 및 에러 처리 규격
프론트엔드와의 원활한 협업을 위해 모든 API 응답은 일관된 봉투(Envelope) 패턴을 사용합니다.

### 4.1. 공통 응답 (`ApiResponse`)
* **규칙 1 (통일성):** 모든 Controller의 반환형은 `ApiResponse<T>`로 통일합니다.
* **규칙 2 (데이터 유무):** 반환할 데이터가 있으면 `ApiResponse.success(data)`, 없으면(POST, DELETE 등) `ApiResponse.successWithNoContent()`를 반환합니다.
* **규칙 3 (메시지 직관성):** `message` 필드는 프론트엔드가 사용자에게 알림창(Toast, Alert)으로 바로 띄워줄 수 있도록 한글로 친절하게 작성합니다.

### 4.2. 예외 처리 (`CustomException` & `ErrorCode`)
* **규칙 1 (에러 던지기):** 비즈니스 로직(Service 계층)에서 에러가 발생하면, `try-catch`로 덮어두지 말고 `throw new CustomException(ErrorCode.XXX)` 형태로 예외를 명시적으로 던집니다.
* **규칙 2 (에러 사전 등록):** 새로운 에러 상황이 생기면, 무조건 `ErrorCode` Enum 파일에 새로운 코드를 추가한 뒤에 사용합니다.

## 5. 아키텍처 및 패키지 구조 (계층별 책임)
우리 프로젝트는 **도메인 주도 패키지 구조(Domain-driven package structure)**를 따르며, 각 계층(Layer)의 책임과 데이터 전달 방식을 엄격하게 분리합니다.

### 5.1 패키지 구조 (도메인별 분리)
* 기능(Controller, Service...)이 아니라 **도메인(Facility, User...)**을 기준으로 패키지를 나눕니다.
* 예시:
  com.barrierfree.bf
  ├── domain
  │   └── facility          // 시설 도메인
  │       ├── controller    // API 진입점
  │       ├── service       // 비즈니스 로직
  │       ├── repository    // DB 접근
  │       ├── entity        // DB 테이블 매핑 객체
  │       └── dto           // 계층 간 데이터 전달 객체 (Request, Response)
  └── global                // 전역 공통 설정 (Exception, Response, Config 등)

### 5.2 계층 간 데이터 전달 규칙 (Entity vs DTO)
가장 중요한 핵심 규칙입니다. **Entity는 절대 Controller 밖(프론트엔드 응답)으로 나가지 않습니다.**

* **Controller**: HTTP 요청/응답을 처리합니다. DTO를 받아 Service로 넘기고, Service가 반환한 데이터를 `ApiResponse`로 감싸서 반환합니다.
* **Service**: 핵심 비즈니스 로직을 담당합니다. Repository에서 Entity를 가져와 조작한 뒤, **반드시 DTO로 변환하여 Controller로 반환**합니다.
* **Repository**: DB와 직접 소통하며 오직 Entity만 다룹니다.

### 5.3 DTO 변환 주체
* Entity -> DTO 변환이나 DTO -> Entity 변환 로직은 **DTO 내부의 팩토리 메서드(또는 빌더)**에서 처리하는 것을 권장합니다.
* (예: `FacilityResponse.from(Facility facility)` 또는 `facilityRequest.toEntity()`)

### 5.4 의존성 주입 (DI) 규칙
* `@Autowired` 필드 주입은 지양합니다.
* 클래스 상단에 `@RequiredArgsConstructor`를 붙이고, 주입받을 객체를 `private final`로 선언하는 **생성자 주입 방식**만 사용합니다.

## 6. 테스트 및 배포 (CI/CD) 규칙

안정적인 서비스 운영을 위해 자동화된 테스트와 배포 파이프라인을 구축하고 준수합니다.

### 6.1 테스트 코드 작성 룰
* **테스트 대상**: Controller(API 스펙)와 Service(비즈니스 로직)는 필수적으로 단위/통합 테스트를 작성합니다.
* **메서드 네이밍 (한국어 허용)**: 테스트의 목적과 상황을 직관적으로 알 수 있도록 **한국어 메서드명**을 적극 권장합니다.
  * 예시: `@Test void 구글맵_API_타임아웃_시_CustomException_발생()`
* **Given-When-Then 패턴**: 모든 테스트 코드는 아래 세 구역으로 주석을 나누어 작성합니다.
  * `// given`: 테스트에 필요한 데이터, 모킹(Mocking) 설정
  * `// when`: 실제 테스트할 메서드 호출
  * `// then`: 예상 결과 검증 (AssertJ 사용)

### 6.2 CI/CD 및 브랜치 배포 전략 (GitHub Flow)
우리 프로젝트는 단일 메인 브랜치(`main`)를 운영하는 GitHub Flow 전략을 따릅니다.

* **CI (지속적 통합 - 자동 테스트)**
  * `main` 브랜치로 PR(Pull Request)이 생성되거나 커밋이 추가되면, GitHub Actions가 자동으로 빌드 및 테스트(`spotlessCheck`, `test`)를 수행합니다.
  * 테스트가 실패한 PR은 `main` 브랜치에 머지할 수 없습니다.
* **CD (지속적 배포 - 자동 배포)**
  * PR이 승인되어 `main` 브랜치로 코드가 머지되면, GitHub Actions가 이를 감지하여 운영(또는 개발) 서버에 자동으로 최신 버전을 배포합니다.
  * 개발자는 별도의 수동 배포 과정을 거치지 않습니다.