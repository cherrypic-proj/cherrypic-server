package org.cherrypic.domain.image.repository;

import static org.cherrypic.event.entity.QEventImage.eventImage;
import static org.cherrypic.image.entity.QImage.image;
import static org.cherrypic.tempalbum.entity.QTempAlbumImage.tempAlbumImage;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;
import org.cherrypic.image.entity.Image;
import org.cherrypic.tempalbum.entity.TempAlbumImage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Slice<EventImageListResponse> findAllByEventId(
            Long eventId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction) {

        Expression<LocalDateTime> sortedField =
                parameter == SortParameter.UPLOAD ? image.createdAt : image.generatedAt;

        List<EventImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        EventImageListResponse.class,
                                        eventImage.id,
                                        image.url,
                                        sortedField))
                        .from(eventImage)
                        .join(eventImage.image, image)
                        .where(
                                eventImage.event.id.eq(eventId),
                                lastImageIdCondition(lastImageId, direction, parameter))
                        .orderBy(getOrderByExpression(parameter, direction))
                        .limit((long) size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public Slice<AlbumImageListResponse> findAllByAlbumId(
            Long albumId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction) {

        Expression<LocalDateTime> sortedField =
                parameter == SortParameter.UPLOAD ? image.createdAt : image.generatedAt;

        List<AlbumImageListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        AlbumImageListResponse.class,
                                        image.id,
                                        image.url,
                                        sortedField))
                        .from(image)
                        .where(
                                image.album.id.eq(albumId),
                                lastImageIdCondition(lastImageId, direction, parameter))
                        .orderBy(getOrderByExpression(parameter, direction))
                        .limit((long) size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public List<Image> findAllUnmappedToEvent(Long eventId, List<Long> imageIds) {

        return queryFactory
                .selectFrom(image)
                .leftJoin(eventImage)
                .on(eventImage.image.id.eq(image.id).and(eventImage.event.id.eq(eventId)))
                .where(image.id.in(imageIds), eventImage.id.isNull())
                .fetch();
    }

    @Override
    public void bulkInsertImages(List<Image> images) {
        String sql =
                "INSERT INTO image (album_id, member_id, url, capacity_mb, generated_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(
                sql,
                images,
                100,
                (ps, image) -> {
                    ps.setLong(1, image.getAlbum().getId());
                    ps.setLong(2, image.getMemberId());
                    ps.setString(3, image.getUrl());
                    ps.setBigDecimal(4, image.getCapacityMb());
                    ps.setObject(5, image.getGeneratedAt());
                });
    }

    @Override
    public void bulkInsertTempAlbumImages(List<TempAlbumImage> images) {
        String sql =
                "INSERT INTO temp_album_image (temp_album_id, url, capacity_mb, created_at, updated_at) "
                        + "VALUES (?, ?, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(
                sql,
                images,
                100,
                (ps, image) -> {
                    ps.setLong(1, image.getTempAlbum().getId());
                    ps.setString(2, image.getUrl());
                    ps.setBigDecimal(3, image.getCapacityMb());
                });
    }

    @Override
    public List<Long> findTempImageIdsByUrlsInOrder(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }

        var cases = new CaseBuilder().when(tempAlbumImage.url.eq(urls.get(0))).then(0);

        for (int i = 1; i < urls.size(); i++) {
            cases = cases.when(tempAlbumImage.url.eq(urls.get(i))).then(i);
        }
        NumberExpression<Integer> orderExpr = cases.otherwise(999_999);

        return queryFactory
                .select(tempAlbumImage.id)
                .from(tempAlbumImage)
                .where(tempAlbumImage.url.in(urls))
                .orderBy(orderExpr.asc())
                .fetch();
    }

    @Override
    public List<Long> findImageIdsByUrlsInOrder(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }

        var cases = new CaseBuilder().when(image.url.eq(urls.get(0))).then(0);

        for (int i = 1; i < urls.size(); i++) {
            cases = cases.when(image.url.eq(urls.get(i))).then(i);
        }
        NumberExpression<Integer> orderExpr = cases.otherwise(999_999);

        return queryFactory
                .select(image.id)
                .from(image)
                .where(image.url.in(urls))
                .orderBy(orderExpr.asc())
                .fetch();
    }

    private BooleanExpression lastImageIdCondition(
            Long imageId, SortDirection direction, SortParameter parameter) {
        if (imageId == null) {
            return null;
        }

        if (parameter == SortParameter.UPLOAD) {
            return direction == SortDirection.DESC ? image.id.lt(imageId) : image.id.gt(imageId);
        } else { // GENERATED
            LocalDateTime lastGeneratedAt = getLastGeneratedAt(imageId);
            return lastGeneratedAtCondition(lastGeneratedAt, direction, parameter);
        }
    }

    private BooleanExpression lastGeneratedAtCondition(
            LocalDateTime lastGeneratedAt, SortDirection direction, SortParameter parameter) {
        if (lastGeneratedAt == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? image.generatedAt.lt(lastGeneratedAt)
                : image.generatedAt.gt(lastGeneratedAt);
    }

    private com.querydsl.core.types.OrderSpecifier<?> getOrderByExpression(
            SortParameter parameter, SortDirection direction) {
        if (parameter == SortParameter.UPLOAD) {
            return direction == SortDirection.DESC ? image.id.desc() : image.id.asc();
        } else { // GENERATED
            return direction == SortDirection.DESC
                    ? image.generatedAt.desc()
                    : image.generatedAt.asc();
        }
    }

    private LocalDateTime getLastGeneratedAt(Long imageId) {
        return queryFactory
                .select(image.generatedAt)
                .from(image)
                .where(image.id.eq(imageId))
                .fetchOne();
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
