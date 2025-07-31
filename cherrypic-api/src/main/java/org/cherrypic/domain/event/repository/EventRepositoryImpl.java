package org.cherrypic.domain.event.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.event.dto.EventListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<EventListResponse> findAllByAlbumId(
            Long albumId, Long lastEventId, int size, SortDirection direction) {
        return null;
    }
}
