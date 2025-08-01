package org.cherrypic.domain.event.repository;

import static org.cherrypic.album.entity.QAlbum.album;
import static org.cherrypic.event.entity.QEvent.event;
import static org.cherrypic.event.entity.QEventImage.eventImage;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.event.dto.EventListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<EventListResponse> findAllByAlbumId(
            Long albumId, Long lastEventId, int size, SortDirection direction) {

        List<EventListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        EventListResponse.class,
                                        event.id,
                                        event.title,
                                        event.coverUrl,
                                        eventImage.count()))
                        .from(eventImage)
                        .join(eventImage.event, event)
                        .where(
                                event.album.id.eq(albumId),
                                lastEventIdCondition(lastEventId, direction))
                        .groupBy(event.id)
                        .orderBy(direction == SortDirection.DESC ? album.id.desc() : album.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastEventIdCondition(Long eventId, SortDirection direction) {
        if (eventId == null) {
            return null;
        }

        return direction == SortDirection.DESC ? event.id.lt(eventId) : album.id.gt(eventId);
    }

    private Slice<EventListResponse> checkLastPage(int pageSize, List<EventListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
