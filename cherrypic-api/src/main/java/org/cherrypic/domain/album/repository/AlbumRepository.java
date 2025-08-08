package org.cherrypic.domain.album.repository;

import org.cherrypic.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlbumRepository extends JpaRepository<Album, Long>, AlbumRepositoryCustom {
    @Query("select a.title from Album a where a.id = :id")
    String findTitleById(@Param("id") Long id);
}
