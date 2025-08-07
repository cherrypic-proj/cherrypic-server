package org.cherrypic.domain.image.repository;

import org.cherrypic.domain.image.dto.response.ImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface ImageRepositoryCustom {
    Slice<ImageListResponse> findAllByEventId(
            Long eventId, Long lastImageId, int size, SortDirection direction);

    Slice<ImageListResponse> findAllByAlbumId(
            Long albumId, Long lastImageId, int size, SortDirection direction);
}
