package org.cherrypic.domain.tempalbum.repository;

import static org.cherrypic.tempalbum.entity.QTempAlbumImage.tempAlbumImage;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.response.TempAlbumImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TempAlbumImageRepositoryImpl implements TempAlbumImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<TempAlbumImageListResponse> findAllByTempAlbumId(
            Long tempAlbumId, Long lastTempAlbumImageId, int size, SortDirection direction) {

        List<TempAlbumImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        TempAlbumImageListResponse.class,
                                        tempAlbumImage.id,
                                        tempAlbumImage.url,
                                        tempAlbumImage.createdAt))
                        .from(tempAlbumImage)
                        .where(
                                tempAlbumImage.tempAlbum.id.eq(tempAlbumId),
                                lastTempAlbumImageIdCondition(lastTempAlbumImageId, direction))
                        .orderBy(
                                direction == SortDirection.DESC
                                        ? tempAlbumImage.id.desc()
                                        : tempAlbumImage.id.asc())
                        .limit((long) size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastTempAlbumImageIdCondition(
            Long tempAlbumImageId, SortDirection direction) {
        if (tempAlbumImageId == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? tempAlbumImage.id.lt(tempAlbumImageId)
                : tempAlbumImage.id.gt(tempAlbumImageId);
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
