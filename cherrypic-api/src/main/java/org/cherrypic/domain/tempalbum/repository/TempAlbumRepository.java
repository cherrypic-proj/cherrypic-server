package org.cherrypic.domain.tempalbum.repository;

import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TempAlbumRepository extends JpaRepository<TempAlbum, Long> {

    long countByMemberId(Long memberId);

    @Query("select t from TempAlbum t where t.member.id = :memberId order by t.id desc")
    List<TempAlbum> findAllByMemberIdOrderByIdDesc(@Param("memberId") Long memberId);
}
