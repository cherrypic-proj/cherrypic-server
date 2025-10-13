package org.cherrypic.domain.tempalbum.repository;

import java.time.LocalDate;
import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TempAlbumRepository extends JpaRepository<TempAlbum, Long> {

    @Query("SELECT t FROM TempAlbum t WHERE t.expiredAt = :now")
    List<TempAlbum> findAllExpiredToday(@Param("now") LocalDate now);
}
