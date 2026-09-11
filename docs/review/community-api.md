# 리뷰 API 변경 및 Swagger 검증

브랜치: `feat/review-community-api` (작업 시작 시 체크아웃된 `fix/kakao-login-internal-error` 기준)

## 변경 계약

- 작성·수정·모든 리뷰 응답에서 `rating` 제거. 조회의 `minRating`과 평점 정렬도 제거.
- 공통 `ReviewResponse`에 `category`(장소 카테고리 enum), `region`(장소 지역), `helpfulCount`(도움 수, 없으면 0) 추가.
- 전체·특정 장소·내가 작성한·도움 표시한·자연어 검색 리뷰 모두 동일한 응답 사용.
- 전체 조회에 `region` 필터 추가. `서울`은 `서울` 및 `서울 용산구` 등 하위 지역, `서울 용산구`는 해당 지역 및 하위 지역과 일치. 단순 부분 문자열 검색은 하지 않음.
- 작성 시 `region` 필수. 앞뒤/중복 공백 정규화. 예: `서울 용산구`, `부산 해운대구`, `경기 수원시`.
- 지역은 요청으로 전달받은 장소 스냅샷. 주소 API를 호출해 자동 확인하지 않음. 프론트의 선택값과 작성값은 **동일한 지역 표기**를 사용해야 함 (`서울`과 `서울특별시`는 별칭으로 자동 변환하지 않음).
- 작성 요청의 `placeId`는 생략 가능하며 경로의 ID를 사용. 기존 클라이언트가 본문에도 보낼 경우 경로와 같아야 함.
- `DELETE /api/v1/reviews/{reviewId}` 추가. 작성자만 소프트 삭제 가능. 삭제 이후 모든 목록·검색·시설 통계에서 제외하며 수정/도움 표시 시 404. 삭제 이력과 연결된 이미지는 보존.
- 수정은 기존 JSON 요청과 새 multipart 요청 모두 지원. 본문 `content` 필수. `mobilities`, `facilities` 미전송/null은 빈 목록 처리(기존 동작).
- 수정 `retainedImageUrls`: 생략/null=기존 모두 유지, `[]`=기존 모두 제거, URL 배열=해당 리뷰의 선택한 이미지만 유지. 다른 리뷰/외부 URL과 중복 URL은 400.
- 새 이미지는 multipart `images`로 추가. 최종 이미지=유지 목록+새 이미지. 이미지 순서 재정렬 자체는 별도 보장하지 않음.
- 수정 `region` 생략/null=기존 유지. 전달 시 지역 보완 가능하며 공백 값은 400.
- 수정 시 내용·시설 기반 임베딩 재생성. 따라서 수정에도 Jina 설정/연결이 필요.
- 교체로 빠진 이미지는 DB 커밋 후 R2에서 삭제. 업로드 후 롤백하면 새 업로드 정리. R2 삭제 실패는 공통 ImageService 로그에 기록.
- 도움 수는 페이지의 리뷰 ID를 한 번에 집계하여 리뷰별 COUNT 쿼리를 방지.
- 일반 조회 정렬은 `createdAt` 또는 `id`만 허용. 기본 최신순. `sort=rating,desc`는 400.
- `UserProfileResponse`는 변경하지 않음. 마이페이지 리뷰 정보는 `/users/me/reviews`와 `/users/me/reviews/helpful` 응답에 포함.

## DB 적용

`src/main/resources/db/migration/V4__update_review_community_fields.sql`을 새 코드 실행 전에 적용해야 한다.
기본 설정은 Flyway 비활성화(`SPRING_FLYWAY_ENABLED=false`)이므로 배포 환경의 마이그레이션 관리 방식에 맞춰 실행한다.
기존 Flyway 관리 환경에서는 활성화 후 V4를 적용하고, 수동 관리 환경에서는 V4 SQL을 실행한다. 신규 Flyway 활성화 시 이전 V1~V3 적용 이력을 먼저 확인한다.

V4는 `region` 컬럼/인덱스와 도움 집계 인덱스를 추가하고 `rating` 컬럼을 제거한다. `ddl-auto=update`만으로는 기존 rating NOT NULL 컬럼이 제거되지 않아 신규 리뷰 INSERT가 실패할 수 있다. 이전 앱 버전이 rating을 사용하므로 DB 변경과 앱 배포를 함께 진행한다.

기존 리뷰의 지역은 NULL이다. 전체/장소/마이페이지에는 조회되지만 지역 필터에는 포함되지 않는다. 검증된 장소 정보로 별도 백필하거나 작성자 수정 API로 보완한다. 장소명만으로 지역을 추정하지 않는다.

## Swagger 확인 순서

Swagger UI에서 작성자 A로 Authorize. 도움 수/권한 확인용 사용자 B도 준비한다.
기본 경로는 `/api/v1`. 작성/수정에서 multipart의 `request` 파트 Content-Type은 `application/json`이다.

1. **작성**: `POST /places/{placeId}/reviews`. `rating` 없이 아래 JSON과 선택적 `images` 파일 전송. 성공 후 `/users/me/reviews`에서 reviewId 확인.

   ```json
   {
     "placeName": "테스트 장소",
     "category": "FOOD",
     "region": "서울 용산구",
     "content": "단차 없이 이용할 수 있었어요.",
     "mobilities": ["WHEELCHAIR"],
     "facilities": ["RAMP"]
   }
   ```

2. **필수값**: `region` 또는 `content`를 빼거나 공백으로 보내면 400. 잘못된 카테고리/이동 유형/시설 코드도 400.
3. **모든 목록**: 아래에서 리뷰별 `category`, `region`, `helpfulCount`와 `rating` 부재 확인. 도움 전에는 0.

   - `GET /reviews`
   - `GET /places/{placeId}/reviews`
   - `GET /users/me/reviews`
   - `GET /users/me/reviews/helpful` (도움 표시 이후)
   - `GET /reviews/search?query=단차 없이 이용`

4. **지역**: 서울 용산구·서울 강남구·부산 해운대구 리뷰를 준비. `GET /reviews?region=서울`은 서울 2개, `region=서울 용산구`는 해당 리뷰, `region=부산`은 부산 리뷰 확인. 미지정은 전체. `category`, `mobilities`, `facilities`와 복합 필터 및 `page=0&size=1`의 totalElements도 확인.
5. **도움 수**: B로 `POST /reviews/{reviewId}/helpful` 후 모든 관련 목록에서 +1 확인. 같은 요청 반복 시 증가하지 않는지 확인. `DELETE /reviews/{reviewId}/helpful` 후 -1 및 B의 도움 목록 제외 확인.
6. **본문 수정**: A로 `PATCH /reviews/{reviewId}`, Content-Type `application/json` 선택. `{"content":"수정한 후기","mobilities":["WHEELCHAIR"],"facilities":["RAMP"]}` 전송. 기존 이미지/지역 유지, 본문 변경 확인.
7. **이미지 수정**: 같은 PATCH에서 `multipart/form-data` 선택. 아래 request와 새 `images` 파일 전송. 유지 URL은 실제 조회된 해당 리뷰 이미지 URL로 교체.

   ```json
   {
     "content": "사진도 변경한 후기",
     "mobilities": ["WHEELCHAIR"],
     "facilities": ["RAMP"],
     "retainedImageUrls": ["기존 이미지 중 유지할 실제 URL"],
     "region": "서울 용산구"
   }
   ```

   유지한 이미지+새 이미지 확인. `retainedImageUrls: []`와 새 파일 없음은 전체 이미지 제거, 해당 필드 생략과 새 파일은 기존 유지+추가. 다른 리뷰 이미지 URL을 넣으면 400.
8. **권한/삭제**: B가 A 리뷰 수정·삭제 시 403. A가 `DELETE /reviews/{reviewId}` 후 성공 확인. 전체·장소·내 리뷰·B 도움 목록·검색에서 제외 및 시설 통계 감소 확인. 삭제 리뷰 수정/도움 표시/재삭제는 404. 비로그인 수정·삭제는 현재 보안 설정에 따라 403.
9. **평점 제거**: Swagger 요청/응답에 rating 및 minRating이 없는지, `sort=rating,desc` 요청은 400인지 확인.

## 자동 검증 범위

리뷰 서비스/MockMvc 테스트는 작성 검증, 공통 응답·집계, 지역 파라미터 전달, 수정 및 소유권, 삭제, multipart/JSON 호환, 이미지 커밋/롤백 처리를 검증한다.
실제 PostgreSQL/pgvector의 필터 SQL·마이그레이션 실행, Jina 및 R2 연동은 별도 실행 환경에서 위 Swagger 절차로 확인해야 한다.

실행 결과: 리뷰 테스트 18개 통과. 전체 85개 중 82개 통과, 기존 `BfBeApplicationTests` 1개와 `OrsRouteServiceTest` 2개는 `DB_URL` 미설정으로 컨텍스트 초기화 실패. 해당 환경 의존 테스트를 제외한 실행은 성공. 리뷰 변경 파일의 Spotless 검사 및 `git diff --check` 통과.
