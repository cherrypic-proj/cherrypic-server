package org.cherrypic.domain.tempalbum.repository;

import static org.cherrypic.image.entity.QImage.image;
import static org.cherrypic.tempalbum.entity.QTempAlbumImage.tempAlbumImage;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.tempalbum.entity.TempAlbumImage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TempAlbumImageRepositoryImpl implements TempAlbumImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void bulkInsertTempAlbumImages(List<TempAlbumImage> images) {
        String sql =
                "INSERT INTO temp_album_image (temp_album_id, url, capacity_gb, created_at, updated_at) "
                        + "VALUES (?, ?, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(
                sql,
                images,
                100,
                (ps, image) -> {
                    ps.setLong(1, image.getTempAlbum().getId());
                    ps.setString(2, image.getUrl());
                    ps.setBigDecimal(3, image.getCapacityGb());
                });
    }

    @Override
    public List<Long> findIdsByUrlsInOrder(List<String> urls) {
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
}
