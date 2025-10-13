package org.cherrypic.domain.album.repository;

import org.cherrypic.album.entity.AlbumParticipationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumParticipationHistoryRepository
        extends JpaRepository<AlbumParticipationHistory, Long>,
                AlbumParticipationHistoryRepositoryCustom {}
