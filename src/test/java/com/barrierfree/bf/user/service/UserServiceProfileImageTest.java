package com.barrierfree.bf.user.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barrierfree.bf.global.auth.JwtProvider;
import com.barrierfree.bf.global.service.ImageService;
import com.barrierfree.bf.user.entity.User;
import com.barrierfree.bf.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class UserServiceProfileImageTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final UserTermService userTermService = mock(UserTermService.class);
  private final JwtProvider jwtProvider = mock(JwtProvider.class);
  private final ImageService imageService = mock(ImageService.class);
  private final UserService userService =
      new UserService(userRepository, userTermService, jwtProvider, imageService);

  @BeforeEach
  void initializeTransactionSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clearTransactionSynchronization() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void deletesReplacedImageOnlyAfterCommit() {
    User user = mock(User.class);
    MockMultipartFile image = profileImage();
    ImageService.UploadedImage uploadedImage =
        new ImageService.UploadedImage("profiles/new.png", "https://cdn.example/profiles/new.png");

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(user.getProfileImageUrl()).thenReturn("https://cdn.example/profiles/old.png");
    when(imageService.extractObjectKey("https://cdn.example/profiles/old.png"))
        .thenReturn("profiles/old.png");
    when(imageService.uploadImage("profiles", image)).thenReturn(uploadedImage);

    userService.updateMyProfileImage(1L, image);

    TransactionSynchronization synchronization = registeredSynchronization();
    verify(imageService, never()).deleteImage("profiles/old.png");

    synchronization.afterCommit();
    synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

    verify(imageService).deleteImage("profiles/old.png");
    verify(imageService, never()).deleteImage("profiles/new.png");
  }

  @Test
  void deletesNewImageWhenTransactionRollsBack() {
    User user = mock(User.class);
    MockMultipartFile image = profileImage();
    ImageService.UploadedImage uploadedImage =
        new ImageService.UploadedImage("profiles/new.png", "https://cdn.example/profiles/new.png");

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(user.getProfileImageUrl()).thenReturn("https://cdn.example/profiles/old.png");
    when(imageService.extractObjectKey("https://cdn.example/profiles/old.png"))
        .thenReturn("profiles/old.png");
    when(imageService.uploadImage("profiles", image)).thenReturn(uploadedImage);

    userService.updateMyProfileImage(1L, image);

    registeredSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(imageService).deleteImage("profiles/new.png");
    verify(imageService, never()).deleteImage("profiles/old.png");
  }

  @Test
  void registersNewImageCleanupBeforeUpdatingUser() {
    User user = mock(User.class);
    MockMultipartFile image = profileImage();
    ImageService.UploadedImage uploadedImage =
        new ImageService.UploadedImage("profiles/new.png", "https://cdn.example/profiles/new.png");

    when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
    when(imageService.uploadImage("profiles", image)).thenReturn(uploadedImage);
    doThrow(new IllegalStateException("update failed"))
        .when(user)
        .updateProfileImage(uploadedImage.publicUrl());

    assertThrows(IllegalStateException.class, () -> userService.updateMyProfileImage(1L, image));

    registeredSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

    verify(imageService).deleteImage("profiles/new.png");
  }

  private TransactionSynchronization registeredSynchronization() {
    return TransactionSynchronizationManager.getSynchronizations().getFirst();
  }

  private MockMultipartFile profileImage() {
    return new MockMultipartFile("image", "profile.png", "image/png", "image-data".getBytes());
  }
}
