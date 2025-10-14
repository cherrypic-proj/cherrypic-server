package org.cherrypic.domain.tempalbum.repository;

import java.time.LocalDate;
import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TempAlbumRepository extends JpaRepository<TempAlbum, Long> {

    @Query("select t.id from TempAlbum t where t.expiredAt = :now")
    List<Long> findAllExpiredIdsToday(@Param("now") LocalDate now);
}
