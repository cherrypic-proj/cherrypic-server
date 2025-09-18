package org.cherrypic.domain.participant.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.favorites.repository.FavoritesRepository;
import org.cherrypic.domain.notification.repository.NotificationRepository;
import org.cherrypic.domain.participant.dto.request.ParticipantRoleUpdateRequest;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.springframework.data.domain.Slice;
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
    private final SubscriptionRepository subscriptionRepository;
    private final FavoritesRepository favoritesRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public void leaveAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        final Participant participant =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        validateNotAlbumHost(participant);

        try {
            favoritesRepository.deleteByParticipantId(participant.getId());
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
            favoritesRepository.deleteByParticipantId(target.getId());
            participantRepository.delete(target);
            notificationRepository.deleteByReceiverIdAndAlbumId(
                    target.getMember().getId(), album.getId());
        } catch (ObjectOptimisticLockingFailureException ignored) {
        }
    }

    @Override
    public void updateParticipantRole(
            Long albumId, Long participantId, ParticipantRoleUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        final Participant requester =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());
        final Participant target = getParticipantById(participantId);

        validateAlbumHost(requester);

        validateSubscriptionNotExpired(album);
        validatePermissionControlAvailable(album);

        validateSelfRoleChange(requester, target);
        validateParticipantBelongsToAlbum(target, album);

        final ParticipantRole newRole = request.role();

        validateNotSameRole(target, newRole);

        target.changeRole(newRole);
        if (newRole == ParticipantRole.HOST) {
            validateSubscriptionInactive(album);
            requester.changeRole(ParticipantRole.STANDARD);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<ParticipantListResponse> getParticipants(
            Long albumId, String lastNickname, Long lastParticipantId, int size) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        final Participant currentParticipant =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        if ((lastNickname == null) != (lastParticipantId == null)) {
            throw new CustomException(ParticipantErrorCode.MISSING_CURSOR_PAIR);
        }

        List<ParticipantListResponse> results = new ArrayList<>();
        int adjustedSize = size;

        if (lastNickname == null) {
            results.add(ParticipantListResponse.from(currentParticipant));
            adjustedSize = size - 1;

            if (adjustedSize == 0) {
                boolean hasNext =
                        participantRepository.countByAlbumIdAndMemberIdNot(
                                        albumId, currentMember.getId())
                                > 0;
                return new SliceResponse<>(results, !hasNext);
            }
        }

        Slice<ParticipantListResponse> paged =
                participantRepository.findParticipantsByAlbumIdExcludingMemberId(
                        albumId,
                        currentMember.getId(),
                        lastNickname,
                        lastParticipantId,
                        adjustedSize);

        results.addAll(paged.getContent());

        return new SliceResponse<>(results, paged.isLast());
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

    private Subscription getSubscriptionByAlbumId(Long albumId) {
        return subscriptionRepository
                .findByAlbumId(albumId)
                .orElseThrow(
                        () -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
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

    private void validatePermissionControlAvailable(Album album) {
        if (album.getType() == AlbumType.BASIC || !album.getPermissionControl()) {
            throw new CustomException(AlbumErrorCode.PERMISSION_CONTROL_NOT_AVAILABLE);
        }
    }

    private void validateSelfRoleChange(Participant requester, Participant target) {
        if (requester.getId().equals(target.getId())) {
            throw new CustomException(AlbumErrorCode.HOST_SELF_ROLE_CHANGE_NOT_ALLOWED);
        }
    }

    private void validateParticipantBelongsToAlbum(Participant target, Album album) {
        if (!target.getAlbum().getId().equals(album.getId())) {
            throw new CustomException(AlbumErrorCode.PARTICIPANT_NOT_IN_ALBUM);
        }
    }

    private void validateSubscriptionInactive(Album album) {
        if (album.getType() == AlbumType.BASIC) return;

        Subscription subscription = getSubscriptionByAlbumId(album.getId());
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new CustomException(AlbumErrorCode.SUBSCRIPTION_ACTIVE_HOST_TRANSFER_NOT_ALLOWED);
        }
    }

    private void validateNotSameRole(Participant target, ParticipantRole role) {
        if (target.getRole() == role) {
            throw new CustomException(ParticipantErrorCode.ROLE_ALREADY_ASSIGNED);
        }
    }

    private void validateSubscriptionNotExpired(Album album) {
        if (album.getType() == AlbumType.BASIC) return;

        Subscription subscription = getSubscriptionByAlbumId(album.getId());
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION);
        }
    }
}
