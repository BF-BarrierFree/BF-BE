package com.barrierfree.bf.review.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.review.service.ReviewService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReviewControllerTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private ReviewService service;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private CacheManager cacheManager;

  private void login() {
    when(jwtProvider.validateAccessToken("test-token")).thenReturn(true);
    when(jwtProvider.getUserIdFromToken("test-token")).thenReturn(1L);
    when(jwtProvider.getRoleFromToken("test-token")).thenReturn("ROLE_USER");
  }

  private MockMultipartFile request(String json) {
    return new MockMultipartFile(
        "request", "", "application/json", json.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void createAcceptsRequestWithoutRatingOrDuplicatePlaceId() throws Exception {
    login();
    mvc.perform(
            multipart("/api/v1/places/place/reviews")
                .file(
                    request(
                        """
          {"placeName":"장소","category":"FOOD","region":"서울 용산구","content":"후기"}
          """))
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());
    verify(service)
        .createReview(eq(1L), eq("place"), argThat(r -> r.getRegion().equals("서울 용산구")), isNull());
  }

  @Test
  void createValidatesRequiredRegion() throws Exception {
    login();
    mvc.perform(
            multipart("/api/v1/places/place/reviews")
                .file(
                    request(
                        """
          {"placeName":"장소","category":"FOOD","content":"후기"}
          """))
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void multipartPatchAcceptsImagesAndRetainedUrls() throws Exception {
    login();
    mvc.perform(
            multipart("/api/v1/reviews/10")
                .file(
                    request(
                        """
          {"content":"수정 후기","retainedImageUrls":[]}
          """))
                .file(new MockMultipartFile("images", "new.png", "image/png", new byte[] {1}))
                .with(
                    r -> {
                      r.setMethod("PATCH");
                      return r;
                    })
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());
    verify(service)
        .updateReview(
            eq(1L),
            eq(10L),
            argThat(r -> r.retainedImageUrls().isEmpty()),
            argThat(files -> files.size() == 1));
  }

  @Test
  void jsonPatchRemainsSupported() throws Exception {
    login();
    mvc.perform(
            patch("/api/v1/reviews/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"수정 후기\"}")
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());
    verify(service)
        .updateReview(eq(1L), eq(10L), argThat(r -> r.retainedImageUrls() == null), isNull());
  }

  @Test
  void deleteRequiresAuthentication() throws Exception {
    mvc.perform(delete("/api/v1/reviews/10")).andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void deleteRoutesAuthenticatedOwnerId() throws Exception {
    login();
    mvc.perform(delete("/api/v1/reviews/10").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());
    verify(service).deleteReview(1L, 10L);
  }

  @Test
  void globalListAcceptsRegionWithoutAuthentication() throws Exception {
    when(service.getAllReviews(isNull(), eq("서울"), isNull(), isNull(), any()))
        .thenReturn(Page.empty());
    mvc.perform(get("/api/v1/reviews").param("region", "서울")).andExpect(status().isOk());
    verify(service).getAllReviews(isNull(), eq("서울"), isNull(), isNull(), any());
  }
}
