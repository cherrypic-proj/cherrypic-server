package org.cherrypic.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.repository.AlbumParticipationHistoryRepository;
import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationHistoryQueryService {

    private final MemberUtil memberUtil;
    private final AlbumParticipationHistoryRepository albumParticipationHistoryRepository;

    public SliceResponse<ParticipationHistoryResponse> getParticipationHistory(
            Long lastHistoryId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();

        Slice<ParticipationHistoryResponse> results =
                albumParticipationHistoryRepository.findParticipationHistory(
                        currentMember.getId(), lastHistoryId, size, direction);

        return SliceResponse.from(results);
    }
}
