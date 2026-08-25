package com.barrierfree.bf.place.repository;

import com.barrierfree.bf.place.entity.SavedPlace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

  List<SavedPlace> findAllByPlaceListIdOrderByCreatedAtDesc(Long placeListId);

  Optional<SavedPlace> findByPlaceListIdAndPlaceId(Long placeListId, String placeId);
}
