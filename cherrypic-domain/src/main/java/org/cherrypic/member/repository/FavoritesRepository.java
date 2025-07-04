package org.cherrypic.member.repository;

import org.cherrypic.member.entity.Favorites;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritesRepository extends JpaRepository<Long, Favorites> {}
