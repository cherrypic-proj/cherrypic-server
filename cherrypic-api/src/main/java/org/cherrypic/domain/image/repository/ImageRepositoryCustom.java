package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;
import org.cherrypic.image.entity.Image;
import org.cherrypic.tempalbum.entity.TempAlbumImage;
import org.springframework.data.domain.Slice;

public interface ImageRepositoryCustom {
    Slice<EventImageListResponse> findAllByEventId(
            Long eventId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction);

    Slice<AlbumImageListResponse> findAllByAlbumId(
            Long albumId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction);

    List<Image> findAllUnmappedToEvent(Long eventId, List<Long> imageIds);

    void bulkInsertImages(List<Image> images);

    void bulkInsertTempAlbumImages(List<TempAlbumImage> images);

    List<Long> findImageIdsByUrlsInOrder(List<String> urls);

    List<Long> findTempImageIdsByUrlsInOrder(List<String> urls);
}
