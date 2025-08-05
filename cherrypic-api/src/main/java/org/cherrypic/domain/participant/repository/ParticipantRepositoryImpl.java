package org.cherrypic.domain.participant.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryImpl implements ParticipantRepositoryCustom {

    private final EntityManager em;
}
