package org.cherrypic.domain.image.repository;

import static org.cherrypic.event.entity.QEventImage.eventImage;
import static org.cherrypic.image.entity.QImage.image;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.image.entity.Image;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<EventImageListResponse> findAllByEventId(
            Long eventId, Long lastImageId, int size, SortDirection direction) {

        List<EventImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        EventImageListResponse.class, eventImage.id, image.url))
                        .from(eventImage)
                        .join(eventImage.image, image)
                        .where(
                                eventImage.event.id.eq(eventId),
                                lastImageIdCondition(lastImageId, direction))
                        .orderBy(direction == SortDirection.DESC ? image.id.desc() : image.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public Slice<AlbumImageListResponse> findAllByAlbumId(
            Long albumId, Long lastImageId, int size, SortDirection direction) {

        List<AlbumImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        AlbumImageListResponse.class, image.id, image.url))
                        .from(image)
                        .where(
                                image.album.id.eq(albumId),
                                lastImageIdCondition(lastImageId, direction))
                        .orderBy(direction == SortDirection.DESC ? image.id.desc() : image.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public List<Image> findAllUnmappedToEvent(Long eventId, Iterable<Long> imageIds) {

        return queryFactory
                .selectFrom(image)
                .leftJoin(eventImage)
                .on(eventImage.image.id.eq(image.id).and(eventImage.event.id.eq(eventId)))
                .where(image.id.in(imageIds), eventImage.id.isNull())
                .fetch();
    }

    private BooleanExpression lastImageIdCondition(Long imageId, SortDirection direction) {
        if (imageId == null) {
            return null;
        }

        return direction == SortDirection.DESC ? image.id.lt(imageId) : image.id.gt(imageId);
    }

    private <T> Slice<T> checkLastPage(int pageSize, List<T> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
