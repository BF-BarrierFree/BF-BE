package com.barrierfree.bf.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.enums.Role;
import com.barrierfree.bf.global.service.ImageService;
import com.barrierfree.bf.user.dto.UserProfileResponse;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  @Test
  void includesUpdatedAtInMyProfile() {
    UserRepository userRepository = mock(UserRepository.class);
    User user = mock(User.class);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 10, 12, 30);
    UserService userService =
        new UserService(
            userRepository,
            mock(UserTermService.class),
            mock(JwtProvider.class),
            mock(ImageService.class));

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(user.getNickname()).thenReturn("barrierfree");
    when(user.getProfileImageUrl()).thenReturn("https://cdn.example/profile.png");
    when(user.getUpdatedAt()).thenReturn(updatedAt);
    when(user.getRole()).thenReturn(Role.USER);

    UserProfileResponse response = userService.getMyProfile(1L);

    assertEquals(updatedAt, response.getUpdatedAt());
  }
}
