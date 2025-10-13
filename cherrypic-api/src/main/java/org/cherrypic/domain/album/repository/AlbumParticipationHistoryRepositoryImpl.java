package org.cherrypic.domain.album.repository;

import static org.cherrypic.album.entity.QAlbumParticipationHistory.albumParticipationHistory;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlbumParticipationHistoryRepositoryImpl
        implements AlbumParticipationHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ParticipationHistoryResponse> findParticipationHistory(
            Long memberId, Long lastHistoryId, int size, SortDirection direction) {
        List<ParticipationHistoryResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        ParticipationHistoryResponse.class,
                                        albumParticipationHistory.id,
                                        albumParticipationHistory.albumTitleSnapshot,
                                        albumParticipationHistory.action,
                                        albumParticipationHistory.createdAt))
                        .from(albumParticipationHistory)
                        .where(
                                albumParticipationHistory.memberId.eq(memberId),
                                lastHistoryIdCondition(lastHistoryId, direction))
                        .orderBy(
                                direction == SortDirection.DESC
                                        ? albumParticipationHistory.id.desc()
                                        : albumParticipationHistory.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastHistoryIdCondition(Long historyId, SortDirection direction) {
        if (historyId == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? albumParticipationHistory.id.lt(historyId)
                : albumParticipationHistory.id.gt(historyId);
    }

    private Slice<ParticipationHistoryResponse> checkLastPage(
            int pageSize, List<ParticipationHistoryResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
