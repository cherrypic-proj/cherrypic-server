package org.cherrypic.domain.participant.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.notification.repository.NotificationRepository;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantServiceImpl implements ParticipantService {

    private final MemberUtil memberUtil;

    private final ParticipantRepository participantRepository;
    private final AlbumRepository albumRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void leaveAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        final Participant participant =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        validateNotAlbumHost(participant);

        try {
            participantRepository.delete(participant);
            notificationRepository.deleteByReceiverIdAndAlbumId(currentMember.getId(), albumId);
        } catch (ObjectOptimisticLockingFailureException ignored) {
        }
    }

    @Override
    public void kickParticipant(Long albumId, Long participantId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        final Participant requester =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());
        final Participant target = getParticipantById(participantId);

        validateAlbumHost(requester);
        validateSelfKick(requester, target);
        validateParticipantBelongsToAlbum(target, album);

        try {
            participantRepository.delete(target);
            notificationRepository.deleteByReceiverIdAndAlbumId(
                    target.getMember().getId(), album.getId());
        } catch (ObjectOptimisticLockingFailureException ignored) {
        }
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

    private Participant getParticipantById(Long participantId) {
        return participantRepository
                .findById(participantId)
                .orElseThrow(() -> new CustomException(ParticipantErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private void validateNotAlbumHost(Participant participant) {
        if (participant.getRole() == ParticipantRole.HOST) {
            throw new CustomException(AlbumErrorCode.HOST_LEAVE_NOT_ALLOWED);
        }
    }

    private void validateAlbumHost(Participant participant) {
        if (participant.getRole() != ParticipantRole.HOST) {
            throw new CustomException(AlbumErrorCode.NOT_ALBUM_HOST);
        }
    }

    private void validateSelfKick(Participant requester, Participant target) {
        if (requester.getId().equals(target.getId())) {
            throw new CustomException(AlbumErrorCode.HOST_SELF_KICK_NOT_ALLOWED);
        }
    }

    private void validateParticipantBelongsToAlbum(Participant target, Album album) {
        if (!target.getAlbum().getId().equals(album.getId())) {
            throw new CustomException(AlbumErrorCode.PARTICIPANT_NOT_IN_ALBUM);
        }
    }
}
