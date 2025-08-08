package org.cherrypic.domain.participant.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByMemberIdAndAlbumId(Long memberId, Long albumId);

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    "update participant set role = 'STANDARD' where album_id = :albumId and role = 'LIMITED'",
            nativeQuery = true)
    void bulkChangeLimitedToStandard(@Param("albumId") Long albumId);

    @Query(
            "select p.member.id from Participant p where p.album.id = :albumId and p.member.id <> :memberId")
    List<Long> findOtherParticipantMemberIds(
            @Param("albumId") Long albumId, @Param("memberId") Long memberId);
}
