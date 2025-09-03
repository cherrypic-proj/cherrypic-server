package org.cherrypic.domain.tempalbum.service;

import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateResponse;

public interface TempAlbumService {

    TempAlbumCreateResponse createTempAlbum(TempAlbumCreateRequest request);
}
