package org.cherrypic.domain.tempalbum.service;

import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;

public interface TempAlbumService {

    void createTempAlbum(Long albumId, TempAlbumCreateRequest request);
}
