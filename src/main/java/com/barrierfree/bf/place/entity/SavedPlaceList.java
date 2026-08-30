package com.barrierfree.bf.place.entity;

import com.barrierfree.bf.global.entity.BaseEntity;
import com.barrierfree.bf.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "saved_place_lists",
    indexes = {@Index(name = "idx_saved_place_list_user_id", columnList = "user_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlaceList extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 50)
  private String name;

  public SavedPlaceList(User user, String name) {
    this.user = user;
    this.name = name;
  }

  public void updateName(String name) {
    this.name = name;
  }
}
