package org.cherrypic.domain.tempalbum.service;

import org.cherrypic.domain.tempalbum.dto.request.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumUpdateRequest;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumCreateResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumInfoResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumListResponse;

public interface TempAlbumService {

    TempAlbumCreateResponse createTempAlbum(TempAlbumCreateRequest request);

    TempAlbumListResponse getTempAlbums();

    void updateTempAlbum(Long tempAlbumId, TempAlbumUpdateRequest request);

    TempAlbumInfoResponse getTempAlbum(Long tempAlbumId);
}
