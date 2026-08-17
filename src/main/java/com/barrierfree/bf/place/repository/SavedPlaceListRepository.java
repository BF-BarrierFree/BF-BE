package com.barrierfree.bf.place.repository;

import com.barrierfree.bf.place.entity.SavedPlaceList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPlaceListRepository extends JpaRepository<SavedPlaceList, Long> {

  List<SavedPlaceList> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<SavedPlaceList> findByIdAndUserId(Long id, Long userId);
}
