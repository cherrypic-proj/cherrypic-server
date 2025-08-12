package org.cherrypic.domain.album.repository;

import org.cherrypic.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long>, AlbumRepositoryCustom {}
