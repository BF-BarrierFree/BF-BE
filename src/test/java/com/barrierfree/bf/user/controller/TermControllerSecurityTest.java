package com.barrierfree.bf.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.user.dto.TermCreateRequest;
import com.barrierfree.bf.user.dto.TermResponse;
import com.barrierfree.bf.user.service.TermService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TermController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TermControllerSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TermService termService;

  @MockitoBean private JwtProvider jwtProvider;

  @MockitoBean private CacheManager cacheManager;

  @Test
  void deniesCreateTermForAuthenticatedNonAdmin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/terms")
                .with(user("user").authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsCreateTermForAdmin() throws Exception {
    when(termService.createTerm(any(TermCreateRequest.class)))
        .thenReturn(
            TermResponse.builder()
                .termKey("PRIVACY_POLICY")
                .title("Privacy policy")
                .content("Policy content")
                .isRequired(true)
                .version(1)
                .build());

    mockMvc
        .perform(
            post("/api/v1/terms")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isOk());
  }

  private String validRequest() {
    return """
        {
          "termKey": "PRIVACY_POLICY",
          "title": "Privacy policy",
          "content": "Policy content",
          "isRequired": true
        }
        """;
  }
}
