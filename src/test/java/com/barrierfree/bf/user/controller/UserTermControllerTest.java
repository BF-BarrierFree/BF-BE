package com.barrierfree.bf.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.user.dto.TermAgreementUpdateRequest;
import com.barrierfree.bf.user.service.UserTermService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserTermController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserTermControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserTermService userTermService;

  @MockitoBean private JwtProvider jwtProvider;

  @MockitoBean private CacheManager cacheManager;

  @BeforeEach
  void setUpAuthentication() {
    when(jwtProvider.validateAccessToken("token")).thenReturn(true);
    when(jwtProvider.getUserIdFromToken("token")).thenReturn(7L);
    when(jwtProvider.getRoleFromToken("token")).thenReturn("ROLE_USER");
  }

  @Test
  void getsAllActiveTermsWithCurrentAgreementStates() throws Exception {
    when(userTermService.getUserAgreements(7L)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/users/me/terms").header("Authorization", "Bearer token"))
        .andExpect(status().isOk());

    verify(userTermService).getUserAgreements(7L);
  }

  @Test
  void updatesOptionalTermAgreement() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/users/me/terms")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreements\":[{\"termId\":11,\"isAgreed\":false}]}"))
        .andExpect(status().isOk());

    verify(userTermService).updateAgreements(eq(7L), any(TermAgreementUpdateRequest.class));
  }
}
