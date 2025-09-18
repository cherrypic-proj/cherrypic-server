package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
    List<Image> findAllById(Iterable<Long> imageIds);

    long countByIdIn(List<Long> ids);

    boolean existsByAlbumId(Long albumId);

    long countByIdInAndAlbumId(List<Long> imageIds, Long albumId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Image i where i.album.id = :albumId")
    void deleteAllByAlbumId(Long albumId);
}
