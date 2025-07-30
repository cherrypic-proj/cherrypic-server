package org.cherrypic.domain.favorites;

import org.cherrypic.favorites.entity.Favorites;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {}
