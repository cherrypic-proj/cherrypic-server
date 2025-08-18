package org.cherrypic.domain.album.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.cherrypic.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlbumRepository extends JpaRepository<Album, Long>, AlbumRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Album a where a.id = :albumId")
    Optional<Album> findByIdWithPessimisticLock(@Param("albumId") Long albumId);
}
