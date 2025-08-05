package org.cherrypic.domain.participant.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryImpl implements ParticipantRepositoryCustom {

    private final EntityManager em;

    @Override
    public void bulkChangeLimitedToStandard(Long albumId) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE participant ")
                .append("SET role = 'STANDARD' ")
                .append("WHERE album_id = ")
                .append(albumId)
                .append(" AND role = 'LIMITED'");

        em.createNativeQuery(sb.toString()).executeUpdate();
    }
}
