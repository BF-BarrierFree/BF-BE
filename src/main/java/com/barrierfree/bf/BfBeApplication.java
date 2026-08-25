package com.barrierfree.bf;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BfBeApplication {

  public static void main(String[] args) {
    // 1. .env 파일 로드 (배포 환경 등 파일이 없으면 무시하도록 설정하여 에러 방지)
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    // 2. .env 내용을 시스템 속성(System Properties)으로 설정
    // 이렇게 하면 application.yaml에서 ${DB_URL} 형태로 바로 가져다 쓸 수 있습니다.
    // OS 환경 변수가 이미 설정되어 있으면 덮어쓰지 않음 (배포 환경 우선)
    dotenv
        .entries()
        .forEach(
            entry -> {
              String key = entry.getKey();
              if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, entry.getValue());
              }
            });

    SpringApplication.run(BfBeApplication.class, args);
  }
}
