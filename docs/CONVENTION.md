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
   - IDE 설정에서 "Class count to use import with '*'" 옵션을 `999` 같은 큰 숫자로 변경하여 자동 변환을 막아주세요.
2. **들여쓰기(Indent)는 무조건 Space 4칸**
   - Java 코드는 Tab 대신 Space 4칸을 사용합니다. (`.editorconfig` 적용 완료)
3. **중괄호 `{ }` 위치 (K&R 스타일)**
   - 클래스와 메서드의 여는 중괄호 `{`는 같은 줄에 작성합니다.
   - 제어문(`if`, `for` 등)의 코드가 한 줄이더라도 반드시 중괄호를 사용합니다.
   ```java
   // ❌ Bad
   if (isTrue)
       doSomething();
       
   // ✅ Good
   if (isTrue) {
       doSomething();
   }
   ```

## 3. 커밋 메시지 컨벤션 (Conventional Commits)

커밋 메시지 작성 시 `.gitmessage.txt` 템플릿을 활용하며, 아래 태그를 준수합니다.

* `feat` : 새로운 기능 추가
* `fix` : 버그 수정
* `docs` : 문서 수정
* `style` : 코드 포맷팅 (로직 변경 없음)
* `refactor` : 코드 리팩토링
* `test` : 테스트 코드 추가/수정
* `chore` : 빌드 설정, 의존성 변경 등