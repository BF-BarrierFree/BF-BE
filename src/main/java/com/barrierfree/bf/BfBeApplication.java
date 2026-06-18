package com.barrierfree.bf;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BfBeApplication {

  public static void main(String[] args) {
    // 1. .env 파일 로드 (배포 환경 등 파일이 없으면 무시하도록 설정하여 에러 방지)
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    // 2. .env 내용을 시스템 속성(System Properties)으로 설정
    // 이렇게 하면 application.yaml에서 ${DB_URL} 형태로 바로 가져다 쓸 수 있습니다.
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

    SpringApplication.run(BfBeApplication.class, args);
  }
}
