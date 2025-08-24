package org.cherrypic.domain.subscription.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final MemberUtil memberUtil;

    private final SubscriptionRepository subscriptionRepository;
    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;

    @Override
    public void cancelSubscription(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateSubscriptionSupported(album.getPlan());

        final Subscription subscription = getSubscriptionByAlbumId(album.getId());

        validateSubscriptionNotExpired(subscription.getEndAt());
        validateSubscriptionNotCanceled(subscription.getStatus());

        subscription.cancelSubscription();
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

    private void validateAlbumHost(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (!participant.getRole().equals(ParticipantRole.HOST)) {
            throw new CustomException(AlbumErrorCode.NOT_ALBUM_HOST);
        }
    }

    private void validateSubscriptionSupported(AlbumPlan plan) {
        if (plan == AlbumPlan.BASIC) {
            throw new CustomException(
                    SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_PLAN);
        }
    }

    private Subscription getSubscriptionByAlbumId(Long albumId) {
        return subscriptionRepository
                .findByAlbumId(albumId)
                .orElseThrow(
                        () -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    private void validateSubscriptionNotCanceled(SubscriptionStatus status) {
        if (status == SubscriptionStatus.CANCELED) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED);
        }
    }

    private void validateSubscriptionNotExpired(LocalDateTime endAt) {
        if (endAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ENDED);
        }
    }
}
