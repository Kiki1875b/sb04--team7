package com.example.ootd.domain.clothes.service;

import com.example.ootd.domain.clothes.dto.data.ClothesDto;
import com.example.ootd.domain.clothes.dto.request.ClothesCreateRequest;
import com.example.ootd.domain.clothes.dto.request.ClothesSearchCondition;
import com.example.ootd.domain.clothes.dto.request.ClothesUpdateRequest;
import com.example.ootd.dto.PageResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * 의상 관리
 */
public interface ClothesService {

  // 옷 등록
  ClothesDto create(ClothesCreateRequest request, MultipartFile image, UUID userId);

  // 옷 수정
  ClothesDto update(ClothesUpdateRequest request, MultipartFile image, UUID clothesId);

  // 옷 목록 조회
  PageResponse<ClothesDto> findByCondition(ClothesSearchCondition condition);

  // 옷 삭제
  void delete(UUID clothesId);
}
