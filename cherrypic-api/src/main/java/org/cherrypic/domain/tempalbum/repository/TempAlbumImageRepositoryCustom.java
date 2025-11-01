package org.cherrypic.domain.tempalbum.repository;

import org.cherrypic.domain.image.dto.response.TempAlbumImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface TempAlbumImageRepositoryCustom {

    Slice<TempAlbumImageListResponse> findAllByTempAlbumId(
            Long tempAlbumId, Long lastTempAlbumImageId, int size, SortDirection direction);
}
