package com.barrierfree.bf.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import com.barrierfree.bf.place.domain.PlaceCategory;
import com.barrierfree.bf.place.dto.PlaceSearchHistoryResponse;
import com.barrierfree.bf.place.repository.PlaceSearchHistoryJdbcRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlaceSearchHistoryServiceTest {

  private final PlaceSearchHistoryJdbcRepository repository =
      Mockito.mock(PlaceSearchHistoryJdbcRepository.class);
  private final PlaceSearchHistoryService service = new PlaceSearchHistoryService(repository);

  @Test
  void deletesSearchHistoryById() {
    service.delete(1L);

    verify(repository).deleteById(1L);
  }

  @Test
  void rejectsInvalidSearchHistoryId() {
    assertThatThrownBy(() -> service.delete(0L))
        .isInstanceOfSatisfying(
            CustomException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

    verify(repository, never()).deleteById(0L);
  }

  @Test
  void deletesAllSearchHistories() {
    service.deleteAll();

    verify(repository).deleteAll();
  }

  @Test
  void returnsRecentSearchHistories() {
    LocalDateTime searchedAt = LocalDateTime.of(2026, 9, 6, 16, 30);
    when(repository.findRecent(10))
        .thenReturn(
            List.of(
                new PlaceSearchHistoryResponse.SearchHistory(
                    1L,
                    "롯데",
                    PlaceCategory.ETC,
                    PlaceCategory.ETC.getLabel(),
                    37.511,
                    127.098,
                    500,
                    searchedAt)));

    PlaceSearchHistoryResponse response = service.getRecent(null);

    assertThat(response.histories()).hasSize(1);
    assertThat(response.histories().getFirst().keyword()).isEqualTo("롯데");
    verify(repository).findRecent(10);
  }
}
