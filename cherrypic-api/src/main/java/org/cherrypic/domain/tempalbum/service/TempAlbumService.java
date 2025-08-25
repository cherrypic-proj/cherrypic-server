package org.cherrypic.domain.tempalbum.service;

import org.cherrypic.domain.album.dto.request.TempAlbumRequest;

public interface TempAlbumService {

    void createTempAlbum(Long albumId, TempAlbumRequest request);
}
