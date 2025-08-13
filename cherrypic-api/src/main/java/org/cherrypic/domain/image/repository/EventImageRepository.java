package org.cherrypic.domain.image.repository;

import org.cherrypic.event.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {}
