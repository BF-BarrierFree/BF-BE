package com.barrierfree.bf.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barrierfree.bf.config.SecurityConfig;
import com.barrierfree.bf.course.domain.CourseDuration;
import com.barrierfree.bf.course.entity.Course;
import com.barrierfree.bf.course.entity.CoursePlace;
import com.barrierfree.bf.course.repository.CourseRepository;
import com.barrierfree.bf.course.service.AiCourseGenerateService;
import com.barrierfree.bf.course.service.CourseService;
import com.barrierfree.bf.global.auth.JwtAuthenticationFilter;
import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.repository.SavedPlaceRepository;
import com.barrierfree.bf.place.service.PlaceService;
import com.barrierfree.bf.route.dto.WheelchairRouteResponse;
import com.barrierfree.bf.route.service.OrsRouteService;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CourseController.class)
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  CourseService.class,
  AiCourseGenerateService.class
})
class CourseControllerTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private CacheManager cacheManager;
  @MockitoBean private CourseRepository courseRepository;
  @MockitoBean private SavedPlaceRepository savedPlaceRepository;
  @MockitoBean private UserRepository userRepository;
  @MockitoBean private OrsRouteService orsRouteService;
  @MockitoBean private PlaceService placeService;

  private void login() {
    when(jwtProvider.validateAccessToken("test-token")).thenReturn(true);
    when(jwtProvider.getUserIdFromToken("test-token")).thenReturn(1L);
    when(jwtProvider.getRoleFromToken("test-token")).thenReturn("ROLE_USER");
  }

  @ParameterizedTest
  @EnumSource(CourseDuration.class)
  void savesPreviewTitleAndAllPlacesWithoutModification(CourseDuration duration) throws Exception {
    // given
    login();
    var user = User.builder().socialId("test-user").nickname("tester").role(Role.USER).build();
    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(orsRouteService.getWheelchairRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new WheelchairRouteResponse(500, 300, List.of(), List.of()));
    AtomicInteger ids = new AtomicInteger();
    when(placeService.search(
            anyString(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            anyInt(),
            isNull(),
            anyList(),
            anyList()))
        .thenAnswer(
            invocation ->
                new PlaceSearchResponse(
                    List.of(
                        mapper.convertValue(
                            Map.of(
                                "placeId",
                                "place-" + ids.incrementAndGet(),
                                "name",
                                "추천 장소",
                                "category",
                                invocation.<String>getArgument(1),
                                "lat",
                                invocation.<Double>getArgument(2),
                                "lng",
                                invocation.<Double>getArgument(3)),
                            PlaceSearchResponse.PlaceSummary.class)),
                    null,
                    false));

    // when
    var previewResult =
        mvc.perform(
                post("/api/v1/courses/ai/preview")
                    .header("Authorization", "Bearer test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "region",
                                "SEOUL",
                                "companion",
                                "FAMILY",
                                "mobilityTypes",
                                List.of(),
                                "theme",
                                "NATURE_HEALING",
                                "duration",
                                duration.name()))))
            .andExpect(status().isOk())
            .andReturn();
    var preview = mapper.readTree(previewResult.getResponse().getContentAsString()).get("data");
    int count = duration.getCompositionRule().size();
    assertThat(preview.get("places").size()).isEqualTo(count);
    var request = mapper.createObjectNode();
    request.set("title", preview.get("courseTitle"));
    request.set("places", preview.get("places"));

    // then
    mvc.perform(
            post("/api/v1/courses/ai")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.places.length()").value(count))
        .andExpect(jsonPath("$.data.places[0].distanceToNext").value("500m"));
    var captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getUser()).isSameAs(user);
    assertThat(saved.isAiGenerated()).isTrue();
    assertThat(saved.getPlaces())
        .extracting(CoursePlace::getSequence)
        .containsExactlyElementsOf(IntStream.range(0, count).boxed().toList());
    assertThat(saved.getPlaces())
        .extracting(CoursePlace::getOriginalPlaceId)
        .containsExactlyElementsOf(
            IntStream.rangeClosed(1, count).mapToObj(i -> "place-" + i).toList());
    assertThat(saved.getPlaces())
        .allSatisfy(place -> assertThat(place.getCourse()).isSameAs(saved));
    assertThat(saved.getPlaces().getLast().getDistanceToNext()).isNull();
  }

  @Test
  void savesMoreThanSevenPlacesEvenWhenRoutingFails() throws Exception {
    // given
    login();
    when(userRepository.findByIdAndIsDeletedFalse(1L))
        .thenReturn(Optional.of(User.builder().build()));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(orsRouteService.getWheelchairRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenThrow(new CustomException(ErrorCode.ROUTE_NOT_FOUND));
    var places =
        IntStream.range(0, 30)
            .mapToObj(
                i ->
                    Map.of(
                        "placeId",
                        "place-" + i,
                        "name",
                        "장소",
                        "latitude",
                        37.55,
                        "longitude",
                        126.97))
            .toList();
    // when / then
    mvc.perform(
            post("/api/v1/courses/ai")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("title", "서울 코스", "places", places))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.places.length()").value(30))
        .andExpect(jsonPath("$.data.places[0].distanceToNext").value("경로 없음"))
        .andExpect(jsonPath("$.data.places[0].category").value("ETC"));
  }

  @Test
  void rejectsEmptyPlaces() throws Exception {
    // given
    login();
    // when / then
    mvc.perform(
            post("/api/v1/courses/ai")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"서울 코스\",\"places\":[]}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(courseRepository);
  }

  @Test
  void requiresAuthenticationToSave() throws Exception {
    // when / then
    mvc.perform(post("/api/v1/courses/ai").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(courseRepository);
  }
}
