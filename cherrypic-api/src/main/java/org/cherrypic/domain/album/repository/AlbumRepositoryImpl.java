package org.cherrypic.domain.album.repository;

import static org.cherrypic.album.entity.QAlbum.album;
import static org.cherrypic.favorites.entity.QFavorites.favorites;
import static org.cherrypic.participant.entity.QParticipant.participant;
import static org.cherrypic.subscription.entity.QSubscription.subscription;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlbumRepositoryImpl implements AlbumRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<AlbumListResponse> findAllByMemberIdWithCondition(
            Long memberId,
            AlbumType type,
            SubscriptionStatus status,
            String keyword,
            Long lastAlbumId,
            int size,
            SortDirection direction) {
        List<Tuple> tuples =
                queryFactory
                        .select(
                                album.id,
                                album.title,
                                album.coverUrl,
                                album.type,
                                subscription.status,
                                album.createdAt,
                                favorites.marked)
                        .from(participant)
                        .join(participant.album, album)
                        .leftJoin(subscription)
                        .on(subscription.album.eq(album))
                        .leftJoin(favorites)
                        .on(favorites.participant.eq(participant))
                        .where(
                                participant.member.id.eq(memberId),
                                type != null ? album.type.eq(type) : null,
                                status != null ? subscription.status.eq(status) : null,
                                keyword != null ? album.title.containsIgnoreCase(keyword) : null,
                                lastAlbumIdCondition(lastAlbumId, direction))
                        .orderBy(direction == SortDirection.DESC ? album.id.desc() : album.id.asc())
                        .limit(size + 1)
                        .fetch();

        List<AlbumListResponse> results =
                tuples.stream()
                        .map(
                                tuple ->
                                        AlbumListResponse.of(
                                                tuple.get(album.id),
                                                tuple.get(album.title),
                                                tuple.get(album.coverUrl),
                                                tuple.get(album.type),
                                                tuple.get(subscription.status),
                                                tuple.get(album.createdAt),
                                                tuple.get(favorites.marked)))
                        .collect(Collectors.toList());

        return checkLastPage(size, results);
    }

    private BooleanExpression lastAlbumIdCondition(Long albumId, SortDirection direction) {
        if (albumId == null) {
            return null;
        }

        return direction == SortDirection.DESC ? album.id.lt(albumId) : album.id.gt(albumId);
    }

    private Slice<AlbumListResponse> checkLastPage(int pageSize, List<AlbumListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
