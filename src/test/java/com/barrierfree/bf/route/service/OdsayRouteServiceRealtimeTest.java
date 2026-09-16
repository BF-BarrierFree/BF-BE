package com.barrierfree.bf.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.route.dto.TransitRouteResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class OdsayRouteServiceRealtimeTest {

  private final WebClient webClient = mock(WebClient.class);
  private final TagoRouteService tagoRouteService = mock(TagoRouteService.class);
  private final SeoulBusRouteService seoulBusRouteService = mock(SeoulBusRouteService.class);
  private final OdsayRouteService odsayRouteService =
      new OdsayRouteService(webClient, tagoRouteService, seoulBusRouteService);

  @Test
  void triesNextRouteWhenFirstRouteHasNoRealtimeInfo() {
    TagoRouteService.RealtimeBusSnapshot snapshot =
        new TagoRouteService.RealtimeBusSnapshot(
            List.of(new TransitRouteResponse.RealtimeArrival("401", 3, "3분 후 도착", 2, null, null)),
            List.of());
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "401"))
        .thenReturn(snapshot);

    TransitRouteResponse response =
        ReflectionTestUtils.invokeMethod(
            odsayRouteService,
            "parseTransitRoute",
            """
            {
              "result": {
                "path": [
                  {
                    "pathType": 1,
                    "info": { "totalTime": 12, "totalDistance": 800, "totalWalk": 800, "payment": 0 },
                    "subPath": [
                      { "trafficType": 3, "startName": "A", "endName": "B", "startX": 126.97, "startY": 37.57, "endX": 126.98, "endY": 37.58 }
                    ]
                  },
                  {
                    "pathType": 2,
                    "info": { "totalTime": 20, "totalDistance": 3000, "totalWalk": 200, "payment": 1500 },
                    "subPath": [
                      {
                        "trafficType": 2,
                        "startName": "광화문", "endName": "강남",
                        "startX": 126.977324, "startY": 37.571407,
                        "endX": 127.027715, "endY": 37.497942,
                        "lane": [{ "name": "401", "busNo": "401", "type": 1, "busID": "1" }]
                      }
                    ]
                  }
                ]
              }
            }
            """);

    assertThat(response).isNotNull();
    assertThat(response.routes()).hasSize(2);
    TransitRouteResponse.Segment secondRouteSegment = response.routes().get(1).segments().get(0);
    assertThat(secondRouteSegment.realtimeAvailable()).isTrue();
    assertThat(secondRouteSegment.realtimeArrivals()).hasSize(1);
  }

  @Test
  void triesNextLaneWhenFirstLaneHasNoRealtimeInfo() {
    TagoRouteService.RealtimeBusSnapshot empty = TagoRouteService.RealtimeBusSnapshot.empty();
    TagoRouteService.RealtimeBusSnapshot snapshot =
        new TagoRouteService.RealtimeBusSnapshot(
            List.of(new TransitRouteResponse.RealtimeArrival("222", 4, "4분 후 도착", 3, null, null)),
            List.of());

    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "111")).thenReturn(empty);
    when(seoulBusRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "111"))
        .thenReturn(empty);
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "222"))
        .thenReturn(snapshot);

    TransitRouteResponse response =
        ReflectionTestUtils.invokeMethod(
            odsayRouteService,
            "parseTransitRoute",
            """
            {
              "result": {
                "path": [
                  {
                    "pathType": 2,
                    "info": { "totalTime": 20, "totalDistance": 3000, "totalWalk": 200, "payment": 1500 },
                    "subPath": [
                      {
                        "trafficType": 2,
                        "startName": "광화문", "endName": "강남",
                        "startX": 126.977324, "startY": 37.571407,
                        "endX": 127.027715, "endY": 37.497942,
                        "lane": [
                          { "name": "111", "busNo": "111", "type": 1, "busID": "1" },
                          { "name": "222", "busNo": "222", "type": 1, "busID": "2" }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """);

    assertThat(response).isNotNull();
    TransitRouteResponse.Segment segment = response.routes().get(0).segments().get(0);
    assertThat(segment.realtimeAvailable()).isTrue();
    assertThat(segment.realtimeArrivals().get(0).busNo()).isEqualTo("222");
    verify(tagoRouteService).getRealtimeBusSnapshot(37.571407, 126.977324, "222");
  }

  @Test
  void mergesRealtimeInfoFromAllMatchingLanes() {
    TagoRouteService.RealtimeBusSnapshot firstSnapshot =
        new TagoRouteService.RealtimeBusSnapshot(
            List.of(new TransitRouteResponse.RealtimeArrival("111", 2, "2분 후 도착", 1, null, null)),
            List.of(new TransitRouteResponse.BusLocation("111", 126.98, 37.57, "광화문")));
    TagoRouteService.RealtimeBusSnapshot secondSnapshot =
        new TagoRouteService.RealtimeBusSnapshot(
            List.of(new TransitRouteResponse.RealtimeArrival("222", 4, "4분 후 도착", 3, null, null)),
            List.of(new TransitRouteResponse.BusLocation("222", 126.99, 37.58, "시청")));

    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "111"))
        .thenReturn(firstSnapshot);
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "222"))
        .thenReturn(secondSnapshot);

    TransitRouteResponse response =
        ReflectionTestUtils.invokeMethod(
            odsayRouteService,
            "parseTransitRoute",
            """
            {
              "result": {
                "path": [
                  {
                    "pathType": 2,
                    "info": { "totalTime": 20, "totalDistance": 3000, "totalWalk": 200, "payment": 1500 },
                    "subPath": [
                      {
                        "trafficType": 2,
                        "startName": "광화문", "endName": "강남",
                        "startX": 126.977324, "startY": 37.571407,
                        "endX": 127.027715, "endY": 37.497942,
                        "lane": [
                          { "name": "111", "busNo": "111", "type": 1, "busID": "1" },
                          { "name": "222", "busNo": "222", "type": 1, "busID": "2" }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """);

    assertThat(response).isNotNull();
    TransitRouteResponse.Segment segment = response.routes().get(0).segments().get(0);
    assertThat(segment.realtimeAvailable()).isTrue();
    assertThat(segment.realtimeArrivals())
        .extracting(TransitRouteResponse.RealtimeArrival::busNo)
        .containsExactly("111", "222");
    assertThat(segment.busLocations())
        .extracting(TransitRouteResponse.BusLocation::busNo)
        .containsExactly("111", "222");
  }

  @Test
  void stopsRealtimeLookupWhenRequestBudgetIsExhausted() {
    TagoRouteService.RealtimeBusSnapshot empty = TagoRouteService.RealtimeBusSnapshot.empty();
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "111")).thenReturn(empty);
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "222")).thenReturn(empty);
    when(tagoRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "333")).thenReturn(empty);
    when(seoulBusRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "111"))
        .thenReturn(empty);
    when(seoulBusRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "222"))
        .thenReturn(empty);
    when(seoulBusRouteService.getRealtimeBusSnapshot(37.571407, 126.977324, "333"))
        .thenReturn(empty);

    TransitRouteResponse response =
        ReflectionTestUtils.invokeMethod(
            odsayRouteService,
            "parseTransitRoute",
            """
            {
              "result": {
                "path": [
                  {
                    "pathType": 2,
                    "info": { "totalTime": 20, "totalDistance": 3000, "totalWalk": 200, "payment": 1500 },
                    "subPath": [
                      {
                        "trafficType": 2,
                        "startName": "광화문", "endName": "강남",
                        "startX": 126.977324, "startY": 37.571407,
                        "endX": 127.027715, "endY": 37.497942,
                        "lane": [
                          { "name": "111", "busNo": "111", "type": 1, "busID": "1" },
                          { "name": "222", "busNo": "222", "type": 1, "busID": "2" },
                          { "name": "333", "busNo": "333", "type": 1, "busID": "3" },
                          { "name": "444", "busNo": "444", "type": 1, "busID": "4" }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """);

    assertThat(response).isNotNull();
    assertThat(response.routes().get(0).segments().get(0).realtimeAvailable()).isFalse();
    verify(tagoRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "111");
    verify(seoulBusRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "111");
    verify(tagoRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "222");
    verify(seoulBusRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "222");
    verify(tagoRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "333");
    verify(seoulBusRouteService, times(1)).getRealtimeBusSnapshot(37.571407, 126.977324, "333");
    verify(tagoRouteService, times(0)).getRealtimeBusSnapshot(37.571407, 126.977324, "444");
    verify(seoulBusRouteService, times(0)).getRealtimeBusSnapshot(37.571407, 126.977324, "444");
  }
}
