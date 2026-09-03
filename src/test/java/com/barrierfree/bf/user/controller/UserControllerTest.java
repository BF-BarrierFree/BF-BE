package com.barrierfree.bf.user.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.user.dto.NicknameCheckResponse;
import com.barrierfree.bf.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
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
    when(userService.checkNicknameAvailability(nickname)).thenReturn(new NicknameCheckResponse(true));

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

  private void assertInvalidNickname(String nickname) throws Exception {
    mockMvc
        .perform(get("/api/v1/users/check-nickname").param("nickname", nickname))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }
}
