package org.cherrypic.domain.album.service;

import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.InvitationLinkCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;

public interface AlbumService {
    AlbumCreateResponse createAlbum(AlbumCreateRequest request);

    InvitationLinkCreateResponse createInvitationLink(InvitationLinkCreateRequest request);
}
