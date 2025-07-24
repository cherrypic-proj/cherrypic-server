package org.cherrypic.domain.album.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.InvitationLinkCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.participant.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumServiceImpl implements AlbumService {

    private final MemberUtil memberUtil;
    private final InvitationLinkService invitationLinkService;

    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;
    private final InvitationCodeRepository invitationCodeRepository;

    @Override
    public AlbumCreateResponse createAlbum(AlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        Album album = Album.createAlbum(request.title(), request.coverUrl());
        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.HOST);
        album.addParticipant(participant);

        albumRepository.save(album);

        return AlbumCreateResponse.from(album);
    }

    @Override
    public InvitationLinkCreateResponse createInvitationLink(InvitationLinkCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateAlbumId(request.albumId());
        validateInvitationAuthority(currentMember.getId(), request.albumId());

        Optional<InvitationCode> invitationCode =
                invitationCodeRepository.findById(request.albumId());
        if (invitationCode.isPresent()) {
            String invitationLink =
                    invitationLinkService.createInvitationLink(invitationCode.get());
            return InvitationLinkCreateResponse.of(invitationLink);
        }

        InvitationCode newCode = invitationLinkService.createInvitationCode(request.albumId());
        invitationCodeRepository.save(newCode);

        String invitationLink = invitationLinkService.createInvitationLink(newCode);

        return InvitationLinkCreateResponse.of(invitationLink);
    }

    private void validateInvitationAuthority(Long memberId, Long albumId) {
        Participant participant =
                participantRepository
                        .findByMemberIdAndAlbumId(memberId, albumId)
                        .orElseThrow(
                                () -> new AlbumException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

        if (!participant.getRole().equals(ParticipantRole.HOST)) {
            throw new AlbumException(AlbumErrorCode.INVALID_INVITATION_AUTHORITY);
        }
    }

    private void validateAlbumId(Long albumId) {
        Optional<Album> album = albumRepository.findById(albumId);

        if (album.isEmpty()) {
            throw new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND);
        }
    }
}
