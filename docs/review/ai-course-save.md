# AI 코스 미리보기 → 저장 확인

장소 수의 상한을 제거했습니다. 최소 1개 장소, 장소별 필수값과 제목 15자 제한은 유지합니다.
자동 생성 제목은 `서울 자연·휴식 코스` 형식으로 저장 가능한 길이로 반환합니다.

## Swagger에서 실제 DB 저장 확인

1. 수정 브랜치가 실행 중인 서버의 Swagger에서 로그인한 사용자의 Access Token으로 Authorize 합니다.
2. `POST /api/v1/courses/ai/preview`에 아래 요청을 전송합니다.

```json
{
  "region": "SEOUL",
  "companion": "FAMILY",
  "mobilityTypes": [],
  "theme": "NATURE_HEALING",
  "duration": "TWO_NIGHTS_MORE"
}
```

3. `POST /api/v1/courses/ai`에 `title`은 응답의 `data.courseTitle`, `places`는 `data.places` 배열 전체를 복사해 전송합니다. 응답 봉투 전체를 보내는 것이 아닙니다. 선택 장소 검색 결과에 따라 생성 수는 달라질 수 있으므로 8개 이상인지 확인합니다.
4. HTTP 200 / `code: SUCCESS`와 `data.id`를 확인합니다. 응답 장소 개수와 순서가 미리보기와 같아야 합니다.
5. `GET /api/v1/courses/{courseId}`로 반환된 ID를 조회해 제목, 장소 개수와 순서가 유지되는지 확인합니다.
6. 테스트 코스가 불필요하면 `DELETE /api/v1/courses/{courseId}`로 삭제합니다.

장소 배열이 비거나 필수 좌표가 없으면 HTTP 400이 정상입니다. 경로 조회가 실패하면 거리에는 `경로 없음` 또는 `거리 계산 실패`가 표시되며 코스 저장은 계속됩니다.

## 자동 테스트 범위

`./gradlew test --tests 'com.barrierfree.bf.course.*'`

- 실제 인증 필터, 컨트롤러와 코스 생성/저장 서비스를 사용하는 MockMvc 테스트
- 모든 일정의 미리보기 제목 및 장소 배열을 그대로 저장 요청으로 전달
- 30개 장소 저장, 경로 조회 실패 시 저장 유지, 빈 장소 배열 거부, 인증 필요 확인
- 사용자 소유 관계, 장소 순서, 마지막 장소의 거리 없음 확인
- 모든 지역/동행자/테마 조합의 자동 제목이 15자 이하인지 확인

DB 저장소, JWT 검증기와 외부 장소/경로 서비스는 모킹합니다. 실제 DB 트랜잭션 및 외부 API 연동은 위 Swagger 절차로 별도 확인해야 합니다.
