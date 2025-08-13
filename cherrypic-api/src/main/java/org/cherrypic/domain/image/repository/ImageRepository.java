package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
    List<Image> findAllById(Iterable<Long> imageIds);

    long countByIdIn(Iterable<Long> ids);

    long countByIdInAndAlbumId(Iterable<Long> imageIds, Long albumId);
}
