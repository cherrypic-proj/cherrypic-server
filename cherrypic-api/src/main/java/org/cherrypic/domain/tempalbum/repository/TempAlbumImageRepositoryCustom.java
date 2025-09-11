package org.cherrypic.domain.tempalbum.repository;

import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbumImage;

public interface TempAlbumImageRepositoryCustom {

    void bulkInsertTempAlbumImages(List<TempAlbumImage> images);

    List<Long> findIdsByUrlsInOrder(List<String> urls);
}
