package org.cherrypic.domain.participant.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository
        extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom {
    Optional<Participant> findByMemberIdAndAlbumId(Long memberId, Long albumId);

    List<Participant> findByAlbumIdAndRole(Long AlbumId, ParticipantRole role);
}
