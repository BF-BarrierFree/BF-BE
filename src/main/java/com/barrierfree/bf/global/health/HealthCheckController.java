package com.barrierfree.bf.global.health;

import com.barrierfree.bf.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

  /**
   * Checks the health status of the server.
   *
   * @return an ApiResponse indicating the server is healthy and ready for database communication
   */
  @GetMapping
  public ApiResponse<String> checkHealth() {
    // ApiResponse의 success(T data, String message) 메서드 활용
    return ApiResponse.success("OK", "서버가 정상적으로 동작 중이며, DB 통신을 대기하고 있습니다.");
  }
}
