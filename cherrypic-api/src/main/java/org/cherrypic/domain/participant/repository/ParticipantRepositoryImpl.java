package org.cherrypic.domain.participant.repository;

import static org.cherrypic.member.entity.QMember.member;
import static org.cherrypic.participant.entity.QParticipant.participant;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
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
            Long albumId, Long lastParticipantId, int size) {
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
                        .where(participant.album.id.eq(albumId))
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
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
