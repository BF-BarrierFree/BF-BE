package com.barrierfree.bf.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WebClientConfigTest {
  @Test
  void decodesPlaceSearchResponseLargerThanDefaultBuffer() throws Exception {
    // given: Google can return successful responses larger than 256 KiB.
    String address = "a".repeat(300 * 1024);
    byte[] body =
        ("{\"places\":[{\"id\":\"place-1\",\"formattedAddress\":\"" + address + "\"}]}")
            .getBytes(StandardCharsets.UTF_8);
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/places",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (var output = exchange.getResponseBody()) {
            output.write(body);
          }
        });
    server.start();
    try {
      // when
      var result =
          new WebClientConfig()
              .webClient()
              .get()
              .uri("http://127.0.0.1:" + server.getAddress().getPort() + "/places")
              .retrieve()
              .bodyToMono(GooglePlaceResponseDto.class)
              .block(Duration.ofSeconds(5));
      // then
      assertThat(result).isNotNull();
      assertThat(result.getPlaces()).hasSize(1);
      assertThat(result.getPlaces().getFirst().getFormattedAddress()).isEqualTo(address);
    } finally {
      server.stop(0);
    }
  }
}
