package org.cherrypic.domain.event.repository;

import java.util.List;
import org.cherrypic.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {
    @Query("select e from Event e where e.album.id = :albumId")
    List<Event> findAllByAlbumId(@Param("albumId") Long albumId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Event e where e.album.id = :albumId")
    void deleteAllByAlbumId(Long albumId);
}
