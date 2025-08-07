package org.cherrypic.domain.image.repository;

import static org.cherrypic.image.entity.QImage.image;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.response.ImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ImageListResponse> findAllByEventId(
            Long eventId, Long lastImageId, int size, SortDirection direction) {

        List<ImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        ImageListResponse.class, image.id, image.url))
                        .from(image)
                        .where(
                                image.event.id.eq(eventId),
                                lastImageIdCondition(lastImageId, direction))
                        .orderBy(direction == SortDirection.DESC ? image.id.desc() : image.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public Slice<ImageListResponse> findAllByAlbumId(
            Long albumId, Long lastImageId, int size, SortDirection direction) {

        List<ImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        ImageListResponse.class, image.id, image.url))
                        .from(image)
                        .where(
                                image.album.id.eq(albumId),
                                lastImageIdCondition(lastImageId, direction))
                        .orderBy(direction == SortDirection.DESC ? image.id.desc() : image.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastImageIdCondition(Long imageId, SortDirection direction) {
        if (imageId == null) {
            return null;
        }

        return direction == SortDirection.DESC ? image.id.lt(imageId) : image.id.gt(imageId);
    }

    private Slice<ImageListResponse> checkLastPage(int pageSize, List<ImageListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
