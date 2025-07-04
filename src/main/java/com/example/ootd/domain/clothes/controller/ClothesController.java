package com.example.ootd.domain.clothes.controller;

import com.example.ootd.domain.clothes.dto.data.ClothesDto;
import com.example.ootd.domain.clothes.dto.request.ClothesCreateRequest;
import com.example.ootd.domain.clothes.dto.request.ClothesSearchCondition;
import com.example.ootd.domain.clothes.dto.request.ClothesUpdateRequest;
import com.example.ootd.domain.clothes.service.ClothesService;
import com.example.ootd.dto.PageResponse;
import com.example.ootd.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

  private final ClothesService clothesService;

  @GetMapping
  public ResponseEntity<PageResponse<ClothesDto>> find(
      @ModelAttribute @Valid ClothesSearchCondition condition) {

    log.info("의상 목록 조회 요청: {}", condition);

    PageResponse<ClothesDto> response = clothesService.findByCondition(condition);

    log.debug("의상 목록 조회 응답: clothesCount={}", response.data().size());

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping
  public ResponseEntity<ClothesDto> create(
      @RequestPart("request") ClothesCreateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image,
      @AuthenticationPrincipal CustomUserDetails userDetails) { // Jwt 사용 시 null이 넘어와서 임시방편으로 CustomUserDetails 사용

    UUID userId = userDetails.getUser().getId();

    log.info("의상 등록 요청: userId={}, request={}", userId, request);

    // request의 ownerId null로 넘어옴 -> userId를 따로 받는 것으로 변경
    ClothesDto response = clothesService.create(request, image, userId);

    log.debug("의상 등록 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @DeleteMapping(path = "/{clothesId}")
  public ResponseEntity<Void> delete(@PathVariable UUID clothesId) {

    log.info("의상 삭제 요청: clothesId={}", clothesId);

    clothesService.delete(clothesId);

    log.debug("의상 삭제 응답 완료");

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @PatchMapping(path = "/{clothesId}")
  public ResponseEntity<ClothesDto> update(
      @PathVariable UUID clothesId,
      @RequestPart(value = "request") ClothesUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image
  ) {

    log.info("의상 수정 요청: clothesId={}, request={}", clothesId, request);

    ClothesDto response = clothesService.update(request, image, clothesId);

    log.debug("의상 수정 응답: {}", response);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
