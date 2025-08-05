package org.cherrypic.domain.event.repository;

import static org.cherrypic.event.entity.QEventImage.eventImage;
import static org.cherrypic.image.entity.QImage.image;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.event.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventImageRepositoryImpl implements EventImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<EventImageListResponse> findAllByEventId(
            Long eventId, Long lastEventImageId, int size, SortDirection direction) {

        List<EventImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        EventImageListResponse.class, eventImage.id, image.url))
                        .from(eventImage)
                        .join(image)
                        .on(eventImage.image.eq(image))
                        .where(
                                eventImage.event.id.eq(eventId),
                                lastEventImageIdCondition(lastEventImageId, direction))
                        .orderBy(
                                direction == SortDirection.DESC
                                        ? eventImage.id.desc()
                                        : eventImage.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastEventImageIdCondition(
            Long eventImageId, SortDirection direction) {
        if (eventImageId == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? eventImage.id.lt(eventImageId)
                : eventImage.id.gt(eventImageId);
    }

    private Slice<EventImageListResponse> checkLastPage(
            int pageSize, List<EventImageListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
