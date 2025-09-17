package org.cherrypic.domain.favorites.repository;

import java.util.Optional;
import org.cherrypic.favorites.entity.Favorites;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {
    Optional<Favorites> findByParticipantId(Long participantId);
}
