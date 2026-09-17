package com.barrierfree.bf.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.barrierfree.bf.auth.controller.AuthController;
import com.barrierfree.bf.auth.dto.AuthResponse;
import com.barrierfree.bf.auth.service.AuthService;
import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.course.controller.CourseController;
import com.barrierfree.bf.course.service.AiCourseGenerateService;
import com.barrierfree.bf.course.service.CourseService;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.inquiry.controller.InquiryController;
import com.barrierfree.bf.inquiry.service.InquiryService;
import com.barrierfree.bf.taxi.batch.TaxiCenterBatchService;
import com.barrierfree.bf.taxi.controller.TaxiReservationController;
import com.barrierfree.bf.taxi.service.TaxiReservationService;
import com.barrierfree.bf.user.controller.UserTermController;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import com.barrierfree.bf.user.service.UserTermService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(
    controllers = {
      AuthController.class,
      TaxiReservationController.class,
      CourseController.class,
      UserTermController.class,
      InquiryController.class
    },
    properties = "jwt.secret=cWEtdGVzdC1vbmx5LXNlY3JldC1mb3Itand0LXNpZ25pbmctMzItYnl0ZXM=")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtProvider.class})
@ActiveProfiles("prod")
class SecurityAccessRegressionTest {
  @Autowired MockMvc mvc;
  @Autowired JwtProvider jwtProvider;
  @Autowired RequestMappingHandlerMapping mappings;
  @MockitoBean UserRepository users;
  @MockitoBean AuthService auth;
  @MockitoBean TaxiReservationService taxis;
  @MockitoBean TaxiCenterBatchService batch;
  @MockitoBean CourseService courses;
  @MockitoBean AiCourseGenerateService aiCourses;
  @MockitoBean UserTermService terms;
  @MockitoBean InquiryService inquiries;
  @MockitoBean CacheManager cacheManager;

  @Test
  void publicNearbyRemainsAccessibleWithoutAuthentication() throws Exception {
    when(taxis.findNearestCenters(37.5, 127.0, 30.0, 3)).thenReturn(List.of());
    mvc.perform(get("/api/v1/taxis/centers/nearby").param("lat", "37.5").param("lng", "127.0"))
        .andExpect(status().isOk());
    verifyNoInteractions(users);
  }

  @Test
  void otherTaxiOperationsStillRequireAuthentication() throws Exception {
    for (String path : List.of("/fare", "/reservations", "/centers/sync")) {
      mvc.perform(
              post("/api/v1/taxis" + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isForbidden());
    }
    verifyNoInteractions(taxis, batch);
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void activeRolesRetainAuthenticatedTaxiAndCourseAccess(Role role) throws Exception {
    String token = tokenFor(role);
    mvc.perform(
            post("/api/v1/taxis/fare")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/taxis/reservations")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservation()))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/v1/courses/9").header("Authorization", token))
        .andExpect(status().isOk());
    verify(taxis).saveReservationHistory(eq(7L), any());
    verify(courses).deleteCourse(7L, 9L);
  }

  @Test
  void acceptsMatchingLegacyUserIdAndRejectsDifferentUserId() throws Exception {
    String token = tokenFor(Role.USER);
    mvc.perform(
            post("/api/v1/taxis/reservations")
                .param("userId", "7")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservation()))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/taxis/reservations")
                .param("userId", "8")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservation()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("G003"));
    verify(taxis, times(1)).saveReservationHistory(eq(7L), any());
    verify(taxis, never()).saveReservationHistory(eq(8L), any());
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void syncIsAdminOnly(Role role) throws Exception {
    var result =
        mvc.perform(post("/api/v1/taxis/centers/sync").header("Authorization", tokenFor(role)));
    if (role == Role.ADMIN) {
      result.andExpect(status().isOk());
      verify(batch).fetchAndUpsertTaxiCenters();
    } else {
      result.andExpect(status().isForbidden());
      verifyNoInteractions(batch);
    }
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void inquiryRolePolicyIsUnchanged(Role role) throws Exception {
    var result = mvc.perform(get("/api/v1/inquiries/my").header("Authorization", tokenFor(role)));
    if (role == Role.GUEST) {
      result.andExpect(status().isForbidden());
      verifyNoInteractions(inquiries);
    } else {
      result.andExpect(status().isOk());
      verify(inquiries).getMyInquiries(eq(7L), any());
    }
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void previouslyValidTokenCannotAccessProtectedOperationsAfterWithdrawal(Role role)
      throws Exception {
    String token = tokenFor(role);
    mvc.perform(get("/api/v1/courses/9").header("Authorization", token)).andExpect(status().isOk());
    clearInvocations(courses);
    when(users.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.empty());
    mvc.perform(get("/api/v1/courses/9").header("Authorization", token))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/courses/9").header("Authorization", token))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/users/me/terms")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreements\":[{\"termId\":1,\"isAgreed\":true}]}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/taxis/reservations")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservation()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/taxis/centers/sync").header("Authorization", token))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/taxis/centers/nearby")
                .param("lat", "37.5")
                .param("lng", "127.0")
                .header("Authorization", token))
        .andExpect(status().isOk());
    verifyNoInteractions(courses, terms, batch);
    verify(taxis, never()).saveReservationHistory(anyLong(), any());
  }

  @Test
  void repositoryFailureDoesNotAuthenticateToken() throws Exception {
    String token = tokenFor(Role.USER);
    when(users.findByIdAndIsDeletedFalse(7L))
        .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
    mvc.perform(delete("/api/v1/courses/9").header("Authorization", token))
        .andExpect(status().isForbidden());
    verifyNoInteractions(courses);
  }

  @Test
  void retiredTestLoginHasNoHandlerAndCannotIssueTokensInProd() throws Exception {
    assertThat(mappings.getHandlerMethods().values())
        .noneMatch(handler -> handler.getMethod().getName().equals("getTestToken"));
    mvc.perform(get("/api/v1/auth/test-login")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/auth/test-login").header("Authorization", tokenFor(Role.ADMIN)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(auth);
    verify(users, never()).findById(anyLong());
    verify(users, never()).save(any());
  }

  @Test
  void normalKakaoLoginRemainsPublicAndKeepsResponseContract() throws Exception {
    when(auth.kakaoLogin("code", "https://client.example/callback"))
        .thenReturn(new AuthResponse("access", "refresh", Role.GUEST, true));
    mvc.perform(
            post("/api/v1/auth/kakao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"code\",\"redirectUri\":\"https://client.example/callback\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("access"))
        .andExpect(jsonPath("$.data.refreshToken").value("refresh"))
        .andExpect(jsonPath("$.data.role").value("GUEST"))
        .andExpect(jsonPath("$.data.isNewUser").value(true));
    verify(auth).kakaoLogin("code", "https://client.example/callback");
    verifyNoInteractions(users);
  }

  private String tokenFor(Role role) {
    User user = User.builder().socialId("test-social").nickname("tester").role(role).build();
    ReflectionTestUtils.setField(user, "id", 7L);
    when(users.findByIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(user));
    return "Bearer " + jwtProvider.generateAccessToken(user);
  }

  private String reservation() {
    return "{\"centerId\":1,\"startAddr\":\"서울역\",\"endAddr\":\"시청\",\"estimatedFare\":\"2500원\"}";
  }
}
