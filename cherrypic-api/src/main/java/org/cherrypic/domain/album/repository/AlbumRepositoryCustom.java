package org.cherrypic.domain.album.repository;

import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.domain.album.dto.response.SubscribedAlbumListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface AlbumRepositoryCustom {
    Slice<AlbumListResponse> findAllByMemberId(
            Long memberId, Long lastAlbumId, int size, SortDirection direction);

    Slice<SubscribedAlbumListResponse> findAllByMemberIdAndPlan(
            Long memberId, AlbumPlan plan, Long lastAlbumId, int size, SortDirection direction);
}
