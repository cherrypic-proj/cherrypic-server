package org.cherrypic.domain.album.service;

import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface AlbumService {
    AlbumCreateResponse createAlbum(AlbumCreateRequest request);

    AlbumUpdateResponse updateAlbum(Long albumId, AlbumUpdateRequest request);

    PermissionToggleResponse togglePermission(Long albumId);

    InvitationLinkCreateResponse createInvitationLink(Long albumId);

    SliceResponse<AlbumListResponse> getParticipatingAlbums(
            Long lastAlbumId, int size, SortDirection direction);

    AlbumJoinResponse joinAlbum(Long albumId, String code);
    void deleteAlbum(Long albumId);
}
