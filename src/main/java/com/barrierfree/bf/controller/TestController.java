package com.barrierfree.bf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

  @GetMapping
  public ApiResponse<String> test() {
    return ApiResponse.success("서버 정상 작동 중!", "Test API 호출 성공");
  }
}
