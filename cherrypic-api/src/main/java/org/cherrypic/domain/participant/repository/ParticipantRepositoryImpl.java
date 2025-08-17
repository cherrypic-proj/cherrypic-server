package org.cherrypic.domain.participant.repository;

import static org.cherrypic.member.entity.QMember.member;
import static org.cherrypic.participant.entity.QParticipant.participant;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryImpl implements ParticipantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ParticipantListResponse> findAllByAlbumId(
            Long albumId, Long lastParticipantId, int size, SortDirection direction) {
        List<ParticipantListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        ParticipantListResponse.class,
                                        participant.id,
                                        member.nickname,
                                        member.profileImageUrl,
                                        participant.role))
                        .from(participant)
                        .join(participant.member, member)
                        .where(
                                participant.album.id.eq(albumId),
                                lastParticipantIdCondition(lastParticipantId, direction))
                        .orderBy(
                                direction == SortDirection.DESC
                                        ? participant.id.desc()
                                        : participant.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastParticipantIdCondition(
            Long participantId, SortDirection direction) {
        if (participantId == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? participant.id.lt(participantId)
                : participant.id.gt(participantId);
    }

    private Slice<ParticipantListResponse> checkLastPage(
            int pageSize, List<ParticipantListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
