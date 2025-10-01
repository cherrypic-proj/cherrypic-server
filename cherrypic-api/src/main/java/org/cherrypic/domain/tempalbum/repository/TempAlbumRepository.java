package org.cherrypic.domain.tempalbum.repository;

import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TempAlbumRepository extends JpaRepository<TempAlbum, Long> {

    long countByMemberId(Long memberId);

    @Query("SELECT t FROM TempAlbum t WHERE t.member.id = :memberId ORDER BY t.id DESC")
    List<TempAlbum> findAllByMemberIdOrderByIdDesc(@Param("memberId") Long memberId);
}
