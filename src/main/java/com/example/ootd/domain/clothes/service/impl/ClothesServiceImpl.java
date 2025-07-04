package com.example.ootd.domain.clothes.service.impl;

import com.example.ootd.domain.clothes.dto.data.ClothesAttributeDto;
import com.example.ootd.domain.clothes.dto.data.ClothesDto;
import com.example.ootd.domain.clothes.dto.request.ClothesCreateRequest;
import com.example.ootd.domain.clothes.dto.request.ClothesSearchCondition;
import com.example.ootd.domain.clothes.dto.request.ClothesUpdateRequest;
import com.example.ootd.domain.clothes.entity.Attribute;
import com.example.ootd.domain.clothes.entity.Clothes;
import com.example.ootd.domain.clothes.entity.ClothesAttribute;
import com.example.ootd.domain.clothes.entity.ClothesType;
import com.example.ootd.domain.clothes.mapper.ClothesMapper;
import com.example.ootd.domain.clothes.repository.AttributeRepository;
import com.example.ootd.domain.clothes.repository.ClothesRepository;
import com.example.ootd.domain.clothes.service.ClothesService;
import com.example.ootd.domain.image.entity.Image;
import com.example.ootd.domain.image.service.ImageService;
import com.example.ootd.domain.user.User;
import com.example.ootd.domain.user.repository.UserRepository;
import com.example.ootd.dto.PageResponse;
import com.example.ootd.exception.clothes.AttributeDetailNotFoundException;
import com.example.ootd.exception.clothes.AttributeNotFoundException;
import com.example.ootd.exception.clothes.ClothesNotFountException;
import com.querydsl.core.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClothesServiceImpl implements ClothesService {

  private final ImageService imageService;
  private final ClothesRepository clothesRepository;
  private final UserRepository userRepository;
  private final AttributeRepository attributeRepository;
  private final ClothesMapper clothesMapper;

  @Override
  public ClothesDto create(ClothesCreateRequest request, MultipartFile image, UUID userId) {

    log.debug("의상 등록 시작: {}", request);

    Image clothesImage = imageService.upload(image);
    User user = userRepository.findById(userId)
        .orElseThrow(); // TODO: null 처리

    // Clothes 등록
    Clothes clothes = Clothes.builder()
        .image(clothesImage)
        .user(user)
        .name(request.name())
        .type(request.type())
        .build();

    // ClothesAttribute 등록 및 Clothes.clothesAttributes에 추가
    setClothesAttributes(clothes, request.attributes());

    clothesRepository.save(clothes);

    ClothesDto response = clothesMapper.toDto(clothes);

    log.info("의상 등록 완료: {}", response);

    return response;
  }

  @Override
  public ClothesDto update(ClothesUpdateRequest request, MultipartFile image, UUID clothesId) {

    log.debug("의상 수정 시작: {}", request);

    Clothes clothes = getClothesById(clothesId);

    updateName(clothes, request.name());
    updateType(clothes, request.type());
    updateImage(clothes, image);
    updateAttribute(clothes, request.attributes());

    ClothesDto response = clothesMapper.toDto(clothes);

    log.info("의상 수정 완료: {}", response);

    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ClothesDto> findByCondition(ClothesSearchCondition condition) {

    log.debug("의상 목록 조회 시작: {}", condition);

    List<Clothes> clothes = clothesRepository.findByCondition(condition);

    // 다음 페이지 없는 경우
    if (clothes.size() <= condition.limit()) {

      PageResponse<ClothesDto> pageResponse = PageResponse.<ClothesDto>builder()
          .data(clothesMapper.toDto(clothes))
          .hasNext(false)
          .nextCursor(null)
          .nextIdAfter(null)
          .sortBy("createdAt")
          .sortDirection("DESCENDING")
          .totalCount(
              clothesRepository.countByCondition(condition.typeEqual(), condition.ownerId()))
          .build();

      log.info("의상 목록 조회 완료: dataCount={}", clothes.size());

      return pageResponse;
    }

    // 다음 페이지 있는 경우
    clothes.remove(clothes.size() - 1); // 다음 페이지 확인용 요소 삭제
    Clothes lastClothes = clothes.get(clothes.size() - 1);
    String nextCursor = lastClothes.getCreatedAt().toString();
    UUID nextIdAfter = lastClothes.getId();

    PageResponse<ClothesDto> pageResponse = PageResponse.<ClothesDto>builder()
        .data(clothesMapper.toDto(clothes))
        .hasNext(true)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .sortBy("createdAt")
        .sortDirection("DESCENDING")
        .totalCount(
            clothesRepository.countByCondition(condition.typeEqual(), condition.ownerId()))
        .build();

    log.info("의상 목록 조회 완료: dataCount={}", clothes.size());

    return pageResponse;
  }

  @Override
  public void delete(UUID clothesId) {

    log.debug("의상 삭제 시작: clothesId={}", clothesId);

    Clothes clothes = getClothesById(clothesId);
    clothesRepository.delete(clothes);

    log.info("의상 삭제 완료");
  }

  private Clothes getClothesById(UUID clothesId) {
    return clothesRepository.findById(clothesId)
        .orElseThrow(() -> ClothesNotFountException.withId(clothesId));
  }

  private void updateName(Clothes clothes, String name) {
    if (!StringUtils.isNullOrEmpty(name)) {
      clothes.updateName(name);
    }
  }

  private void updateType(Clothes clothes, ClothesType type) {
    if (type != null) {
      clothes.updateType(type);
    }
  }

  private void updateImage(Clothes clothes, MultipartFile image) {
    if (image != null && !image.isEmpty()) {
      Image clothesImage = imageService.upload(image);
      clothes.updateImage(clothesImage);
    }
  }

  // 속성 수정
  // TODO: 리팩토링 필요
  private void updateAttribute(Clothes clothes, List<ClothesAttributeDto> dtoList) {
    if (dtoList == null || dtoList.isEmpty()) {
      return;
    }

    Map<UUID, String> incomingAttrMap = toIncomingAttributeMap(dtoList);
    Map<UUID, Attribute> attributeMap = getAttributeMap(dtoList);
    Set<ClothesAttribute> currentAttributes = clothes.getClothesAttributes();

    removeDeletedAttributes(currentAttributes, incomingAttrMap.keySet());
    updateExistingAttributes(currentAttributes, incomingAttrMap, attributeMap);
    addNewAttributes(clothes, currentAttributes, incomingAttrMap, attributeMap);
  }

  // 요청 데이터 map으로 변환
  private Map<UUID, String> toIncomingAttributeMap(List<ClothesAttributeDto> dtoList) {
    return dtoList.stream()
        .collect(Collectors.toMap(
            ClothesAttributeDto::definitionId,
            ClothesAttributeDto::value,
            (v1, v2) -> v2
        ));
  }

  // 삭제 대상 속성 제거, 같은 id의 속성이 이미 존재할 경우
  private void removeDeletedAttributes(Set<ClothesAttribute> currentAttributes,
      Set<UUID> incomingIds) {
    currentAttributes.removeIf(attr -> !incomingIds.contains(attr.getAttribute().getId()));
  }

  // 기존 속성 업데이트, 요청한 속성이 이미 존재하고 value만 달라지는 경우 value만 수정
  private void updateExistingAttributes(Set<ClothesAttribute> currentAttributes,
      Map<UUID, String> incomingAttrMap,
      Map<UUID, Attribute> attributeMap) {
    for (ClothesAttribute existing : currentAttributes) {
      UUID id = existing.getAttribute().getId();
      String newValue = incomingAttrMap.get(id);

      if (!existing.getValue().equals(newValue)) {
        Attribute attribute = attributeMap.get(id);
        validateAttributeValue(attribute, newValue);
        existing.updateValue(newValue);
      }
    }
  }

  // 새로운 속성 추가, 없는 속성일 경우 새로운 속성을 더함
  private void addNewAttributes(Clothes clothes,
      Set<ClothesAttribute> currentAttributes,
      Map<UUID, String> incomingAttrMap,
      Map<UUID, Attribute> attributeMap) {

    Set<UUID> existingIds = currentAttributes.stream()
        .map(attr -> attr.getAttribute().getId())
        .collect(Collectors.toSet());

    for (Map.Entry<UUID, String> entry : incomingAttrMap.entrySet()) {
      UUID id = entry.getKey();

      if (!existingIds.contains(id)) {
        ClothesAttributeDto dto = new ClothesAttributeDto(id, entry.getValue());
        ClothesAttribute newAttr = getClothesAttribute(dto, attributeMap, clothes);
        clothes.addClothesAttribute(newAttr);
      }
    }
  }

  // 값 유효성 검증, 속성 details에 요청한 value가 있는지 검증
  private void validateAttributeValue(Attribute attribute, String value) {
    if (attribute == null) {
      throw AttributeNotFoundException.withId(null);
    }
    if (!attribute.getDetails().contains(value)) {
      throw AttributeDetailNotFoundException.withValue(value);
    }
  }


  private void setClothesAttributes(Clothes clothes, List<ClothesAttributeDto> attributeDtoList) {

    Map<UUID, Attribute> attributeMap = getAttributeMap(attributeDtoList);

    for (ClothesAttributeDto dto : attributeDtoList) {

      ClothesAttribute clothesAttribute = getClothesAttribute(dto, attributeMap, clothes);

      clothes.addClothesAttribute(clothesAttribute);
    }
  }

  private Map<UUID, Attribute> getAttributeMap(List<ClothesAttributeDto> attributeDtoList) {

    List<UUID> clothesAttributeIdList = attributeDtoList.stream()
        .map(ClothesAttributeDto::definitionId)
        .toList();

    List<Attribute> attributeList = attributeRepository.findAllById(clothesAttributeIdList);

    return attributeList.stream()
        .collect(Collectors.toMap(Attribute::getId, Function.identity()));
  }

  private ClothesAttribute getClothesAttribute(ClothesAttributeDto dto,
      Map<UUID, Attribute> attributeMap, Clothes clothes) {

    Attribute attribute = attributeMap.get(dto.definitionId());

    if (attribute == null) {
      throw AttributeNotFoundException.withId(dto.definitionId());
    }
    // 해당 속성에 value(속성 내용)가 없는 경우 예외처리
    if (!attribute.getDetails().contains(dto.value())) {
      throw AttributeDetailNotFoundException.withValue(dto.value());
    }

    return ClothesAttribute.builder()
        .clothes(clothes)
        .attribute(attribute)
        .value(dto.value())
        .build();
  }
}
