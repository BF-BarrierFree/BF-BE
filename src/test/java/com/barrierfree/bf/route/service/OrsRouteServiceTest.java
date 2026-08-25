package com.barrierfree.bf.route.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrsRouteServiceTest {

  // 테스트 컨텍스트가 로드되기 전에 가장 먼저 .env 파일을 읽어 시스템 속성에 주입합니다.
  static {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
  }

  @Autowired private OrsRouteService orsRouteService;

  // 실제 테스트용 좌표: 광화문 광장 (휠체어 이동이 완벽하게 보장되는 평지 플라자)
  private final double START_LNG = 126.977324;
  private final double START_LAT = 37.571407;
  private final double END_LNG = 126.977464;
  private final double END_LAT = 37.570221;

  @Test
  @DisplayName("[테스트 1] 휠체어 경로 요청 시 계단을 회피한 정제 좌표 배열을 정상 반환한다")
  void ORS_휠체어_경로_요청시_정제된_좌표배열을_정상반환한다() {
    // given
    double startLng = START_LNG;
    double startLat = START_LAT;
    double endLng = END_LNG;
    double endLat = END_LAT;

    // when
    WheelchairRouteResponse response =
        orsRouteService.getWheelchairRoute(startLng, startLat, endLng, endLat);

    // then
    assertThat(response).isNotNull();
    assertThat(response.totalDistanceMeter()).isGreaterThan(0);
    assertThat(response.pathCoordinates()).isNotEmpty();

    System.out.println("=== [테스트 1] 정제된 FE 응답 페이로드 ===");
    System.out.println("총 이동 거리(m): " + response.totalDistanceMeter());
    System.out.println("예상 소요 시간(초): " + response.totalDurationSecond());
    System.out.println("추출된 폴리라인 노드 수: " + response.pathCoordinates().size());
    System.out.println("첫 좌표(출발지 부근): " + response.pathCoordinates().get(0));
  }

  @Test
  @DisplayName("[테스트 3] 동일 좌표 연속 호출 시 2번째 요청은 외부 API를 찌르지 않고 캐시에서 즉시 반환한다")
  void 연속호출시_두번째_요청은_캐싱되어_레이턴시가_비약적으로_짧다() {
    // given
    // 1회차 선행 호출 (로컬 메모리 캐시에 적재됨)
    orsRouteService.getWheelchairRoute(START_LNG, START_LAT, END_LNG, END_LAT);

    // when
    long startTime = System.currentTimeMillis();
    // 2회차 호출 (이때는 외부망을 타지 않고 캐시에서 즉시 꺼내옴)
    WheelchairRouteResponse cachedResponse =
        orsRouteService.getWheelchairRoute(START_LNG, START_LAT, END_LNG, END_LAT);
    long endTime = System.currentTimeMillis();

    long latency = endTime - startTime;

    // then
    assertThat(cachedResponse).isNotNull();
    // 메모리 캐싱이 정상 작동했다면 네트워크 I/O가 없으므로 50ms 미만으로 응답해야 함
    assertThat(latency).isLessThan(50L);

    System.out.println("=== [테스트 3] 캐시 히트 레이턴시: " + latency + "ms ===");
  }
}
