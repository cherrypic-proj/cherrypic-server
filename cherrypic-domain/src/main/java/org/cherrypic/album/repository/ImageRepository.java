package org.cherrypic.album.repository;

import org.cherrypic.album.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Long, Image> {}
