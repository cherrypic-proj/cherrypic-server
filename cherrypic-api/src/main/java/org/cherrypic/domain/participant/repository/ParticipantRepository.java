package org.cherrypic.domain.participant.repository;

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
                    "UPDATE participant SET role = 'STANDARD' WHERE album_id = :albumId AND role = 'LIMITED'",
            nativeQuery = true)
    void bulkChangeLimitedToStandard(@Param("albumId") Long albumId);

    boolean existsByAlbumIdAndMemberIdIsNot(Long AlbumId, Long memberId);
}
