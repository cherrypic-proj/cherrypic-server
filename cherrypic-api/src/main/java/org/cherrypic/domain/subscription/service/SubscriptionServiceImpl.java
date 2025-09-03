package org.cherrypic.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.subscription.dto.request.SubscriptionRenewRequest;
import org.cherrypic.domain.subscription.dto.response.SubscriptionInfoResponse;
import org.cherrypic.domain.subscription.dto.response.SubscriptionRenewResponse;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.subscription.entity.Subscription;
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
    private final PaymentRepository paymentRepository;

    @Override
    public void cancelSubscription(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateSubscriptionSupported(album);

        final Subscription subscription = getSubscriptionByAlbumId(album.getId());

        subscription.cancel();
    }

    @Override
    public SubscriptionRenewResponse renewSubscription(
            Long albumId, SubscriptionRenewRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateSubscriptionSupported(album);

        final Payment payment = getPaidPaymentById(request.paymentId());

        validatePaymentMemberMismatch(payment, currentMember);

        payment.updatePayment(PaymentPurpose.RENEWAL, album);

        final Subscription subscription = getSubscriptionByAlbumId(album.getId());

        subscription.renew();

        return SubscriptionRenewResponse.from(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionInfoResponse getSubscriptionInfo(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateSubscriptionSupported(album);

        final Subscription subscription = getSubscriptionByAlbumId(albumId);

        return SubscriptionInfoResponse.from(subscription);
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

    private void validateSubscriptionSupported(Album album) {
        if (album.getType() == AlbumType.BASIC) {
            throw new CustomException(
                    SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE);
        }
    }

    private Subscription getSubscriptionByAlbumId(Long albumId) {
        return subscriptionRepository
                .findByAlbumId(albumId)
                .orElseThrow(
                        () -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    private Payment getPaidPaymentById(Long paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validatePaymentMemberMismatch(Payment payment, Member member) {
        if (!payment.getMember().getId().equals(member.getId())) {
            throw new CustomException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH);
        }
    }
}
