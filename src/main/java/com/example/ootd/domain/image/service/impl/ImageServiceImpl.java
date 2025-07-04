package com.example.ootd.domain.image.service.impl;

import com.example.ootd.domain.image.entity.Image;
import com.example.ootd.domain.image.repository.ImageRepository;
import com.example.ootd.domain.image.service.ImageService;
import com.example.ootd.domain.image.service.S3Service;
import com.example.ootd.exception.image.ImageNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {

  private final S3Service s3Service;
  private final ImageRepository imageRepository;

  @Override
  public Image upload(MultipartFile image) {

    if (image == null) {
      return null;
    }

    Image saveImage = s3Service.save(image);
    imageRepository.save(saveImage);

    return saveImage;
  }

  @Override
  @Transactional(readOnly = true)
  public String read(UUID id) {

    Image image = imageRepository.findById(id)
        .orElseThrow(() -> ImageNotFoundException.withId(id));

    return image.getUrl();
  }

  @Override
  public void delete(UUID id) {

    Image image = imageRepository.findById(id)
        .orElseThrow(() -> ImageNotFoundException.withId(id));
    s3Service.delete(image.getFileName());
    imageRepository.deleteById(id);
  }
}
