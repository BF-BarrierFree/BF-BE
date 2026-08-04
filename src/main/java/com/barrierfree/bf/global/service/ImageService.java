package com.barrierfree.bf.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.barrierfree.bf.global.exception.CustomException;
import com.barrierfree.bf.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cloudflare R2 (S3 호환) 이미지 업로드 및 관리를 담당하는 공통 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloud.cloudflare.r2.public-url}")
    private String publicUrl;

    // 보안 및 렌더링을 위해 허용할 이미지 확장자 목록
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "heic");

    /**
     * 단일 이미지를 R2 스토리지에 업로드하고, 접근 가능한 Public URL을 반환합니다.
     *
     * @param directory 업로드할 폴더명 (예: "reviews", "profiles")
     * @param image     프론트엔드로부터 전달받은 이미지 파일 (MultipartFile)
     * @return 업로드된 이미지의 Public URL (String)
     */
    public String uploadImage(String directory, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null; // 이미지가 필수가 아닐 수 있으므로 빈 값이면 null 반환 (필요시 예외 처리로 변경 가능)
        }

        String originalFilename = image.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        
        validateExtension(extension);

        // 파일명 중복을 막기 위해 UUID를 사용하여 고유한 파일명 생성 (예: reviews/123e4567-e89b-12d3-a456-426614174000.jpg)
        String uniqueFilename = directory + "/" + UUID.randomUUID() + "." + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(image.getContentType());
        metadata.setContentLength(image.getSize());

        try (InputStream inputStream = image.getInputStream()) {
            // R2 버킷에 파일 업로드 요청
            amazonS3.putObject(new PutObjectRequest(bucket, uniqueFilename, inputStream, metadata));
            log.info("R2 이미지 업로드 성공: {}", uniqueFilename);
            
            // 프론트엔드에서 바로 보여줄 수 있는 전체 URL 조합하여 반환
            return publicUrl + "/" + uniqueFilename;
            
        } catch (IOException e) {
            log.error("이미지 업로드 중 IO 예외 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 파일명에서 확장자를 추출합니다.
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 허용된 확장자인지 검증합니다. (악성 스크립트 등 업로드 방지)
     */
    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
    }
}