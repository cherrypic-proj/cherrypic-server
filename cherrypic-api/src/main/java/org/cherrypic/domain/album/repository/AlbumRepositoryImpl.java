package org.cherrypic.domain.album.repository;

import static org.cherrypic.album.entity.QAlbum.album;
import static org.cherrypic.participant.entity.QParticipant.participant;
import static org.cherrypic.subscription.entity.QSubscription.subscription;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.domain.album.dto.response.SubscribedAlbumListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlbumRepositoryImpl implements AlbumRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<AlbumListResponse> findAllByMemberId(
            Long memberId, Long lastAlbumId, int size, SortDirection direction) {
        List<AlbumListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        AlbumListResponse.class,
                                        album.id,
                                        album.title,
                                        album.coverUrl,
                                        album.plan))
                        .from(participant)
                        .join(participant.album, album)
                        .where(
                                participant.member.id.eq(memberId),
                                lastAlbumIdCondition(lastAlbumId, direction))
                        .orderBy(direction == SortDirection.DESC ? album.id.desc() : album.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    @Override
    public Slice<SubscribedAlbumListResponse> findAllByMemberIdAndPlan(
            Long memberId, AlbumPlan plan, Long lastAlbumId, int size, SortDirection direction) {
        List<Tuple> tuples =
                queryFactory
                        .select(
                                album.id,
                                album.title,
                                album.plan,
                                album.createdAt,
                                subscription.startAt,
                                subscription.endAt,
                                subscription.nextBillingAt)
                        .from(participant)
                        .join(participant.album, album)
                        .leftJoin(album.subscription, subscription)
                        .where(
                                participant.member.id.eq(memberId),
                                album.plan.eq(plan),
                                lastAlbumIdCondition(lastAlbumId, direction))
                        .orderBy(direction == SortDirection.DESC ? album.id.desc() : album.id.asc())
                        .limit(size + 1)
                        .fetch();

        List<SubscribedAlbumListResponse> results =
                tuples.stream()
                        .map(
                                tuple ->
                                        SubscribedAlbumListResponse.of(
                                                tuple.get(album.id),
                                                tuple.get(album.title),
                                                tuple.get(album.plan),
                                                tuple.get(album.createdAt),
                                                tuple.get(subscription.startAt),
                                                tuple.get(subscription.endAt),
                                                tuple.get(subscription.nextBillingAt)))
                        .toList();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastAlbumIdCondition(Long albumId, SortDirection direction) {
        if (albumId == null) {
            return null;
        }

        return direction == SortDirection.DESC ? album.id.lt(albumId) : album.id.gt(albumId);
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
