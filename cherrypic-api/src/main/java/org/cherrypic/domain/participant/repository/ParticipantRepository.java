package org.cherrypic.domain.participant.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ParticipantRepository
        extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom {

    @Query("select p from Participant p where p.member.id = :memberId and p.album.id = :albumId")
    Optional<Participant> findByMemberIdAndAlbumId(Long memberId, Long albumId);

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    "update participant set role = 'STANDARD' where album_id = :albumId and role = 'LIMITED'",
            nativeQuery = true)
    void bulkChangeLimitedToStandard(Long albumId);

    @Query(
            "select p.member.id from Participant p where p.album.id = :albumId and p.member.id <> :memberId")
    List<Long> findOtherParticipantMemberIds(Long albumId, Long memberId);

    @Query("select p from Participant p where p.album.id = :albumId and p.role = 'HOST'")
    Optional<Participant> findHostByAlbumId(Long albumId);

    int countByAlbumId(Long albumId);

    long countByAlbumIdAndMemberIdNot(Long albumId, Long excludeMemberId);
}
