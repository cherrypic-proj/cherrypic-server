package org.cherrypic.album.repository;

import org.cherrypic.album.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Long, Participant> {}
