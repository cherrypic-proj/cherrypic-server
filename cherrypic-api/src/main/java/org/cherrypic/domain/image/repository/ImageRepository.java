package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
    List<Image> findAllById(Iterable<Long> imageIds);

    long countByIdIn(List<Long> ids);

    long countByIdInAndAlbumId(List<Long> imageIds, Long albumId);

    List<Image> findByUrlIn(List<String> urls);

    @Query(
            value = "select id from image where url in (:urls) order by field(url, :urls)",
            nativeQuery = true)
    List<Long> findIdsByUrlsInOrder(@Param("urls") List<String> urls);
}
