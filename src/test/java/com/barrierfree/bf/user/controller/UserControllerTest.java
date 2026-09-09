package com.barrierfree.bf.user.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.user.dto.NicknameCheckResponse;
import com.barrierfree.bf.user.dto.UserProfileImageResponse;
import com.barrierfree.bf.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean private JwtProvider jwtProvider;

  @MockitoBean private CacheManager cacheManager;

  @Test
  void acceptsNicknameAtMaximumLength() throws Exception {
    String nickname = "a".repeat(15);
    when(userService.checkNicknameAvailability(nickname))
        .thenReturn(new NicknameCheckResponse(true));

    mockMvc
        .perform(get("/api/v1/users/check-nickname").param("nickname", nickname))
        .andExpect(status().isOk());

    verify(userService).checkNicknameAvailability(nickname);
  }

  @Test
  void rejectsBlankNickname() throws Exception {
    assertInvalidNickname("");
  }

  @Test
  void rejectsWhitespaceOnlyNickname() throws Exception {
    assertInvalidNickname("   ");
  }

  @Test
  void rejectsNicknameOverMaximumLength() throws Exception {
    assertInvalidNickname("a".repeat(16));
  }

  @Test
  void uploadsProfileImageThroughSingleEndpoint() throws Exception {
    Long userId = 1L;
    MockMultipartFile image =
        new MockMultipartFile("image", "profile.png", "image/png", "image-data".getBytes());
    when(userService.updateMyProfileImage(userId, image))
        .thenReturn(
            UserProfileImageResponse.builder()
                .profileImageUrl("https://cdn.example/profiles/profile.png")
                .build());
    when(jwtProvider.validateAccessToken("test-token")).thenReturn(true);
    when(jwtProvider.getUserIdFromToken("test-token")).thenReturn(userId);
    when(jwtProvider.getRoleFromToken("test-token")).thenReturn("ROLE_USER");

    mockMvc
        .perform(
            multipart("/api/v1/users/me/profile-image")
                .file(image)
                .with(
                    request -> {
                      request.setMethod("PATCH");
                      return request;
                    })
                .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    verify(userService).updateMyProfileImage(userId, image);
  }

  private void assertInvalidNickname(String nickname) throws Exception {
    mockMvc
        .perform(get("/api/v1/users/check-nickname").param("nickname", nickname))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }
}
