package org.cherrypic.domain.participant.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository
        extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom {

    @Query("select p from Participant p where p.member.id = :memberId and p.album.id = :albumId")
    Optional<Participant> findByMemberIdAndAlbumId(
            @Param("memberId") Long memberId, @Param("albumId") Long albumId);

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

    @Query("select p from Participant p where p.album.id = :albumId and p.role = 'HOST'")
    Optional<Participant> findHostByAlbumId(@Param("albumId") Long albumId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Participant p where p.album.id = :albumId")
    void deleteAllByAlbumId(Long albumId);

    int countByAlbumId(Long albumId);

    long countByAlbumIdAndMemberIdNot(Long albumId, Long excludeMemberId);
}
