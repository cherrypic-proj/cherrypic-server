package org.cherrypic.domain.tempalbum.repository;

import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempAlbumRepository extends JpaRepository<TempAlbum, Long> {

    long countByMemberId(Long memberId);
}
