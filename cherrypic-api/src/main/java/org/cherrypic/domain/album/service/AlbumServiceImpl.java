package org.cherrypic.domain.album.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.InvitationLinkCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.exception.ParticipantException;
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

        validateInvitationAuthority(currentMember.getId(), request.albumId());

        Optional<InvitationCode> invitationCode =
                invitationCodeRepository.findById(request.albumId());
        if (invitationCode.isPresent()) {
            return InvitationLinkCreateResponse.from(invitationCode.get());
        }

        InvitationCode newCode =
                InvitationCode.builder()
                        .albumId(request.albumId())
                        .code(UUID.randomUUID().toString())
                        .ttl(Duration.ofMinutes(30).getSeconds())
                        .build();
        invitationCodeRepository.save(newCode);

        return InvitationLinkCreateResponse.from(newCode);
    }

    private void validateInvitationAuthority(Long memberId, Long albumId) {
        Participant participant =
                participantRepository
                        .findByMemberIdAndAlbumId(memberId, albumId)
                        .orElseThrow(
                                () ->
                                        new ParticipantException(
                                                ParticipantErrorCode.PARTICIPANT_NOT_FOUND));

        Album album =
                albumRepository
                        .findById(albumId)
                        .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));

        boolean isNotBasic = !album.getPlan().equals(AlbumPlan.BASIC);
        boolean isNotHost = !participant.getRole().equals(ParticipantRole.HOST);

        if (isNotBasic && isNotHost) {
            throw new AlbumException(AlbumErrorCode.INVITATION_NOT_ALLOWED);
        }
    }
}
