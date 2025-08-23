package org.cherrypic.domain.album.repository;

import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface AlbumRepositoryCustom {
    Slice<AlbumListResponse> findAllByMemberIdAndPlanAndKeyword(
            Long memberId,
            AlbumPlan plan,
            String keyword,
            Long lastAlbumId,
            int size,
            SortDirection direction);
}
