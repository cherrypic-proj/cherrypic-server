package org.cherrypic.image.repository;

import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Long, Image> {}
