package org.cherrypic.domain.album.service;

import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;

public interface AlbumService {
    AlbumCreateResponse createAlbum(AlbumCreateRequest request);
}
