package org.cherrypic.album.repository;

import org.cherrypic.album.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Long, Event> {}
