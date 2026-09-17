package com.barrierfree.bf.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.user.entity.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class JwtProviderConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(
              context -> {
                var sources = context.getEnvironment().getPropertySources();
                // Do not consume a developer's or deployment's real secrets in these tests.
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                try {
                  new YamlPropertySourceLoader()
                      .load("application", new ClassPathResource("application.yaml"))
                      .forEach(sources::addLast);
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
              })
          .withUserConfiguration(JwtConfiguration.class);

  @Test
  void missingSecretFailsContextStartup() {
    runner.run(
        context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasStackTraceContaining("JWT_SECRET");
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "c2hvcnQ="})
  void unsafeSecretFailsContextStartup(String secret) {
    runner
        .withPropertyValues("JWT_SECRET=" + secret)
        .run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(ints = {32, 64})
  void configuredSecretIssuesAndValidatesTokens(int bytes) {
    String secret =
        Base64.getEncoder().encodeToString("t".repeat(bytes).getBytes(StandardCharsets.US_ASCII));
    runner
        .withPropertyValues("JWT_SECRET=" + secret)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              JwtProvider provider = context.getBean(JwtProvider.class);
              User user = User.builder().socialId("qa").nickname("tester").role(Role.USER).build();
              ReflectionTestUtils.setField(user, "id", 7L);
              String access = provider.generateAccessToken(user);
              String refresh = provider.generateRefreshToken(user);
              assertThat(provider.validateAccessToken(access)).isTrue();
              assertThat(provider.getUserIdFromToken(access)).isEqualTo(7L);
              assertThat(provider.getRoleFromToken(access)).isEqualTo("ROLE_USER");
              assertThat(provider.validateRefreshToken(refresh)).isTrue();
              assertThat(provider.validateAccessToken(refresh)).isFalse();
              assertThat(provider.validateRefreshToken(access)).isFalse();
              JwtProvider another = new JwtProvider();
              ReflectionTestUtils.setField(
                  another,
                  "secretKeyString",
                  Base64.getEncoder()
                      .encodeToString("u".repeat(bytes).getBytes(StandardCharsets.US_ASCII)));
              another.init();
              assertThat(another.validateAccessToken(access)).isFalse();
            });
  }

  @Configuration(proxyBeanMethods = false)
  @Import(JwtProvider.class)
  static class JwtConfiguration {
    @Bean
    static PropertySourcesPlaceholderConfigurer placeholders() {
      return new PropertySourcesPlaceholderConfigurer();
    }
  }
}
