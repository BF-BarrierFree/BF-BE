package com.barrierfree.bf.place.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.place.domain.PlaceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "saved_places",
    indexes = {
      @Index(name = "idx_saved_place_list_id", columnList = "place_list_id"),
      @Index(name = "idx_saved_place_list_place_id", columnList = "place_list_id, place_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_list_id", nullable = false)
  private SavedPlaceList placeList;

  @Column(name = "place_id", nullable = false, length = 150)
  private String placeId;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private PlaceCategory category;

  private Boolean openNow;

  @Column(length = 500)
  private String address;

  private Double latitude;

  private Double longitude;

  @Column(length = 1000)
  private String photoUrl;

  public SavedPlace(
      SavedPlaceList placeList,
      String placeId,
      String name,
      PlaceCategory category,
      Boolean openNow,
      String address,
      Double latitude,
      Double longitude,
      String photoUrl) {
    this.placeList = placeList;
    this.placeId = placeId;
    updateSnapshot(name, category, openNow, address, latitude, longitude, photoUrl);
  }

  public void updateSnapshot(
      String name,
      PlaceCategory category,
      Boolean openNow,
      String address,
      Double latitude,
      Double longitude,
      String photoUrl) {
    this.name = name;
    this.category = category == null ? PlaceCategory.ETC : category;
    this.openNow = openNow;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.photoUrl = photoUrl;
  }
}
