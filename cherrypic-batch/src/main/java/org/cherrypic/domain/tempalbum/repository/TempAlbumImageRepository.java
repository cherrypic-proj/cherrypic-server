package org.cherrypic.domain.tempalbum.repository;

import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbumImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TempAlbumImageRepository extends JpaRepository<TempAlbumImage, Long> {

    @Modifying(clearAutomatically = true)
    @Query("delete from TempAlbumImage i where i.tempAlbum.id in :albumIds")
    void deleteAllByTempAlbumIds(List<Long> albumIds);
}
