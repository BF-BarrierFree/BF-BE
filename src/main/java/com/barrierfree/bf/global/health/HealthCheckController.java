package com.barrierfree.bf.global.health;

import com.barrierfree.bf.global.response.ApiResponse;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

  private final DataSource dataSource;

  public HealthCheckController(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<String>> checkHealth() {
    try {
      // 실제 데이터베이스 연결 테스트
      dataSource.getConnection().close();
      return ResponseEntity.ok(
          ApiResponse.success("OK", "서버가 정상적으로 동작 중이며, DB 연결이 확인되었습니다."));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(ApiResponse.error("UNHEALTHY", "데이터베이스 연결 실패: " + e.getMessage()));
    }
  }
}
