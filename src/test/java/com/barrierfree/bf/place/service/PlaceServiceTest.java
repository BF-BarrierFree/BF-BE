package com.barrierfree.bf.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.place.dto.GooglePlaceResponseDto;
import com.barrierfree.bf.place.dto.PlaceDetailResponse;
import com.barrierfree.bf.place.dto.PlaceSearchResponse;
import com.barrierfree.bf.place.dto.PublicBarrierFreeInfo;
import com.barrierfree.bf.place.domain.PlaceCategory;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceServiceTest {

  private final PlaceTestService placeTestService = Mockito.mock(PlaceTestService.class);
  private final PlaceSearchHistoryService placeSearchHistoryService =
      Mockito.mock(PlaceSearchHistoryService.class);
  private final TourBarrierFreeService tourBarrierFreeService =
      Mockito.mock(TourBarrierFreeService.class);
  private final PlaceService service =
      new PlaceService(null, placeTestService, placeSearchHistoryService, tourBarrierFreeService);

  @Test
  void includesWeekdayDescriptionsInPlaceDetail() {
    GooglePlaceResponseDto.Place place = new GooglePlaceResponseDto.Place();
    GooglePlaceResponseDto.LocalizedText displayName = new GooglePlaceResponseDto.LocalizedText();
    GooglePlaceResponseDto.Location location = new GooglePlaceResponseDto.Location();
    GooglePlaceResponseDto.OpeningHours openingHours =
        new GooglePlaceResponseDto.OpeningHours();

    ReflectionTestUtils.setField(displayName, "text", "롯데월드");
    ReflectionTestUtils.setField(location, "latitude", 37.511);
    ReflectionTestUtils.setField(location, "longitude", 127.098);
    ReflectionTestUtils.setField(openingHours, "openNow", true);
    ReflectionTestUtils.setField(
        openingHours,
        "weekdayDescriptions",
        List.of("월요일: 오전 10:00~오후 9:00", "화요일: 오전 10:00~오후 9:00"));
    ReflectionTestUtils.setField(place, "id", "place-1");
    ReflectionTestUtils.setField(place, "displayName", displayName);
    ReflectionTestUtils.setField(place, "formattedAddress", "서울 송파구");
    ReflectionTestUtils.setField(place, "location", location);
    ReflectionTestUtils.setField(place, "regularOpeningHours", openingHours);

    when(placeTestService.getPlaceDetails("place-1")).thenReturn(place);
    when(tourBarrierFreeService.findByPlaceNameAndLocation(eq("롯데월드"), any(), any()))
        .thenReturn(PublicBarrierFreeInfo.notFound());

    PlaceDetailResponse response = service.getDetail("place-1");

    assertThat(response.openNow()).isTrue();
    assertThat(response.weekdayDescriptions())
        .containsExactly("월요일: 오전 10:00~오후 9:00", "화요일: 오전 10:00~오후 9:00");
  }
  @Test
  void includesOpeningHoursAndReviewCountInPlaceSearchSummary() {
    GooglePlaceResponseDto.Place place = new GooglePlaceResponseDto.Place();
    GooglePlaceResponseDto.LocalizedText displayName = new GooglePlaceResponseDto.LocalizedText();
    GooglePlaceResponseDto.Location location = new GooglePlaceResponseDto.Location();
    GooglePlaceResponseDto.OpeningHours openingHours =
        new GooglePlaceResponseDto.OpeningHours();

    ReflectionTestUtils.setField(displayName, "text", "Star Cafe");
    ReflectionTestUtils.setField(location, "latitude", 37.5);
    ReflectionTestUtils.setField(location, "longitude", 127.0);
    ReflectionTestUtils.setField(openingHours, "openNow", false);
    ReflectionTestUtils.setField(
        openingHours,
        "weekdayDescriptions",
        List.of("Monday: 10:00 AM-9:00 PM", "Tuesday: 10:00 AM-9:00 PM"));
    ReflectionTestUtils.setField(place, "id", "place-1");
    ReflectionTestUtils.setField(place, "displayName", displayName);
    ReflectionTestUtils.setField(place, "formattedAddress", "Seoul");
    ReflectionTestUtils.setField(place, "location", location);
    ReflectionTestUtils.setField(place, "regularOpeningHours", openingHours);
    ReflectionTestUtils.setField(place, "nationalPhoneNumber", "02-123-4567");
    ReflectionTestUtils.setField(place, "userRatingCount", 123);

    PlaceSearchResponse.PlaceSummary summary =
        ReflectionTestUtils.invokeMethod(
            service, "toPlaceSummary", place, PlaceCategory.CAFE, false, new HashMap<>());

    assertThat(summary.openNow()).isFalse();
    assertThat(summary.phoneNumber()).isEqualTo("02-123-4567");
    assertThat(summary.weekdayDescriptions())
        .containsExactly("Monday: 10:00 AM-9:00 PM", "Tuesday: 10:00 AM-9:00 PM");
    assertThat(summary.reviewCount()).isEqualTo(123);
    verify(tourBarrierFreeService, never()).findByPlaceNameAndLocation(any(), any(), any());
  }
}
