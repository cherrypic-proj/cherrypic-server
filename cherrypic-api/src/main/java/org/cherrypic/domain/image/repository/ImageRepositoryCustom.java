package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.image.entity.Image;
import org.springframework.data.domain.Slice;

public interface ImageRepositoryCustom {
    Slice<EventImageListResponse> findAllByEventId(
            Long eventId, Long lastImageId, int size, SortDirection direction);

    Slice<AlbumImageListResponse> findAllByAlbumId(
            Long albumId, Long lastImageId, int size, SortDirection direction);

    List<Image> findAllUnmappedToEvent(Long eventId, List<Long> imageIds);
}
