package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record InvitationLinkCreateResponse(
        @Schema(
                        description = "엘범 초대 딥링크",
                        example = "https://dev-api.cherrypic.today/participants/join?code=3FA7A9")
                String invitationLink) {

    public static InvitationLinkCreateResponse of(String invitationCode) {
        return new InvitationLinkCreateResponse(invitationCode);
    }
}
