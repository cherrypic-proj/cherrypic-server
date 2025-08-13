package org.cherrypic.domain.participant.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private final MemberUtil memberUtil;

    private final ParticipantRepository participantRepository;
    private final AlbumRepository albumRepository;

    @Override
    public void leaveAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        final Participant participant =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        validateNotAlbumHost(participant);

        participantRepository.delete(participant);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private void validateNotAlbumHost(Participant participant) {
        if (participant.getRole() == ParticipantRole.HOST) {
            throw new CustomException(AlbumErrorCode.HOST_LEAVE_NOT_ALLOWED);
        }
    }
}
