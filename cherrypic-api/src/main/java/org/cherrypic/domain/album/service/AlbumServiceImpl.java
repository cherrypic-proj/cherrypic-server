package org.cherrypic.domain.album.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.participant.repository.ParticipantRepository;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.repository.PaymentRepository;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumServiceImpl implements AlbumService {

    private final MemberUtil memberUtil;
    private final InvitationLinkService invitationLinkService;

    private final AlbumRepository albumRepository;
    private final PaymentRepository paymentRepository;
    private final ParticipantRepository participantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvitationCodeRepository invitationCodeRepository;

    @Override
    public AlbumCreateResponse createAlbum(AlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validatePaymentRequirementForPlan(request.plan(), request.paymentId());

        Album album = Album.createAlbum(request.title(), request.coverUrl(), request.plan());

        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.HOST);
        album.addParticipant(participant);

        if (request.plan() != AlbumPlan.BASIC) {
            final Payment payment = getPaidPaymentById(request.paymentId());

            validatePaidStatus(payment);
            validatePaymentMemberMismatch(currentMember, payment);
            validatePaymentNotUsed(payment);

            album.addPayment(payment);

            Subscription subscription =
                    Subscription.createSubscription(currentMember, album, payment.getPaidAt());
            subscriptionRepository.save(subscription);
        }

        albumRepository.save(album);

        return AlbumCreateResponse.from(album);
    }

    @Override
    public InvitationLinkCreateResponse createInvitationLink(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateInvitationAuthority(currentMember.getId(), album.getId());

        InvitationCode invitationCode =
                invitationCodeRepository
                        .findById(albumId)
                        .orElseGet(
                                () -> {
                                    InvitationCode newCode =
                                            invitationLinkService.createInvitationCode(albumId);
                                    invitationCodeRepository.save(newCode);
                                    return newCode;
                                });

        String invitationLink = invitationLinkService.createInvitationLink(invitationCode);

        return InvitationLinkCreateResponse.of(invitationLink);
    }

    private void validatePaymentRequirementForPlan(AlbumPlan plan, Long paymentId) {
        if (plan.requiresPayment() && paymentId == null) {
            throw new AlbumException(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_PLAN);
        }

        if (!plan.requiresPayment() && paymentId != null) {
            throw new AlbumException(AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN);
        }
    }

    private void validatePaidStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new AlbumException(PaymentErrorCode.NOT_PAID);
        }
    }

    private void validatePaymentMemberMismatch(Member member, Payment payment) {
        if (!payment.getMember().getId().equals(member.getId())) {
            throw new AlbumException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH);
        }
    }

    private void validatePaymentNotUsed(Payment payment) {
        if (albumRepository.existsByPayment(payment)) {
            throw new AlbumException(PaymentErrorCode.ALREADY_USED_PAYMENT);
        }
    }

    private Payment getPaidPaymentById(Long paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new AlbumException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateInvitationAuthority(Long memberId, Long albumId) {
        Participant participant =
                participantRepository
                        .findByMemberIdAndAlbumId(memberId, albumId)
                        .orElseThrow(
                                () -> new AlbumException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

        boolean isHost = participant.getRole().equals(ParticipantRole.HOST);

        if (!isHost) {
            throw new AlbumException(AlbumErrorCode.NOT_ALBUM_HOST);
        }
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }
}
