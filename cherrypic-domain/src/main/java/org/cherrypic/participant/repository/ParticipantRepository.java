package org.cherrypic.participant.repository;

import org.cherrypic.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByMemberIdAndAlbumId(Long memberId, Long albumId);
}
