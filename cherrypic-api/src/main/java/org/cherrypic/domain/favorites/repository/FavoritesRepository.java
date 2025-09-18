package org.cherrypic.domain.favorites.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {
    Optional<Favorites> findByParticipantId(Long participantId);

    void deleteByParticipantId(Long participantId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Favorites f where f.participant in :participants")
    void deleteAllByParticipants(List<Participant> participants);
}
