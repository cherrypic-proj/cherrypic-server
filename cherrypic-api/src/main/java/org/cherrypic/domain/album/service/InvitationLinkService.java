package org.cherrypic.domain.album.service;

import java.time.Duration;
import java.util.UUID;
import org.cherrypic.album.entity.InvitationCode;
import org.springframework.stereotype.Service;

@Service
public class InvitationLinkService {

    private static final String INVITATION_LINK_PREFIX =
            "https://dev-api.cherrypic.today/albums/join?albumId=%d&code=%s";
    private static final int INVITATION_LINK_DURATION = 30;
    private static final int UUID_LENGTH = 8;

    public InvitationCode createInvitationCode(Long albumId) {
        return InvitationCode.builder()
                .albumId(albumId)
                .code(UUID.randomUUID().toString().substring(0, UUID_LENGTH))
                .ttl(Duration.ofMinutes(INVITATION_LINK_DURATION).getSeconds())
                .build();
    }

    public String createInvitationLink(InvitationCode invitationCode) {
        return String.format(
                INVITATION_LINK_PREFIX, invitationCode.getAlbumId(), invitationCode.getCode());
    }
}
