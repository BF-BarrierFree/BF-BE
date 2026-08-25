package com.barrierfree.bf.global.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cloudflare R2 (AWS S3 호환) 통신을 위한 설정 클래스 */
@Configuration
public class S3Config {

  @Value("${cloud.cloudflare.r2.credentials.access-key}")
  private String accessKey;

  @Value("${cloud.cloudflare.r2.credentials.secret-key}")
  private String secretKey;

  @Value("${cloud.cloudflare.r2.endpoint}")
  private String endpoint;

  @Value("${cloud.cloudflare.r2.region}")
  private String region;

  /** R2 버킷과 통신할 AmazonS3 클라이언트를 Bean으로 등록합니다. Service 계층에서 의존성 주입(DI)을 받아 이미지 업로드/삭제에 사용됩니다. */
  @Bean
  public AmazonS3 amazonS3() {
    // 1. 발급받은 Access Key와 Secret Key로 인증 객체 생성
    BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    // 2. Cloudflare R2의 Endpoint URL과 Region 설정
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(endpoint, region);

    // 3. 클라이언트 빌드 및 반환
    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(endpointConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        // R2는 PathStyleAccess를 활성화해야 버킷 이름을 경로에 올바르게 매핑합니다.
        .withPathStyleAccessEnabled(true)
        .build();
  }
}
