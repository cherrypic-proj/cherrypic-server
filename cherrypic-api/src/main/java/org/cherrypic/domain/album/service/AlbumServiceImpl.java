package org.cherrypic.domain.album.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.subscription.entity.Subscription;
import org.springframework.data.domain.Slice;
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

        validatePermissionControl(request.plan(), request.permissionControl());

        Album album =
                Album.createAlbum(
                        request.title(),
                        request.coverUrl(),
                        request.plan(),
                        request.permissionControl());

        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.HOST);
        album.addParticipant(participant);

        if (request.plan() != AlbumPlan.BASIC) {
            final Payment payment = getPaidPaymentById(request.paymentId());

            validatePaidStatus(payment);
            validatePaymentMemberMismatch(payment, currentMember);
            validatePaymentNotUsed(payment);

            payment.updatePayment(album);

            Subscription subscription =
                    Subscription.createSubscription(currentMember, album, payment.getPaidAt());
            subscriptionRepository.save(subscription);
        }

        albumRepository.save(album);

        return AlbumCreateResponse.from(album);
    }

    @Override
    public AlbumUpdateResponse updateAlbum(Long albumId, AlbumUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());

        album.updateAlbum(request.title(), request.coverUrl());

        return AlbumUpdateResponse.from(album);
    }

    @Override
    public PermissionToggleResponse togglePermission(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validatePermissionControl(album.getPlan(), true);

        album.togglePermissionControl();

        if (!album.getPermissionControl()) {
            participantRepository.bulkChangeLimitedToStandard(albumId);
        }

        return PermissionToggleResponse.from(album);
    }

    @Override
    public InvitationLinkCreateResponse createInvitationLink(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());

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

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<AlbumListResponse> getParticipatingAlbums(
            Long lastAlbumId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();

        Slice<AlbumListResponse> results =
                albumRepository.findAllByMemberId(
                        currentMember.getId(), lastAlbumId, size, direction);

        return SliceResponse.from(results);
    }

    @Override
    public AlbumJoinResponse joinAlbum(Long albumId, String code) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        final InvitationCode currentInvitationCode =
                invitationCodeRepository
                        .findById(album.getId())
                        .orElseThrow(
                                () -> new AlbumException(AlbumErrorCode.INVITATION_CODE_NOT_FOUND));

        validateAlbumRejoin(currentMember, album);
        validateInvitationCode(currentInvitationCode, code);

        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.STANDARD);
        participantRepository.save(participant);

        return AlbumJoinResponse.from(participant);
    }

    @Override
    public void deleteAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateRemainingParticipants(album.getId(), currentMember.getId());

        albumRepository.delete(album);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private void validateAlbumHost(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (!participant.getRole().equals(ParticipantRole.HOST)) {
            throw new AlbumException(AlbumErrorCode.NOT_ALBUM_HOST);
        }
    }

    private void validatePaymentRequirementForPlan(AlbumPlan plan, Long paymentId) {
        if (plan.requiresPayment() && paymentId == null) {
            throw new AlbumException(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_PLAN);
        }

        if (!plan.requiresPayment() && paymentId != null) {
            throw new AlbumException(AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN);
        }
    }

    private void validatePermissionControl(AlbumPlan plan, Boolean permissionControl) {
        if (plan == AlbumPlan.BASIC && permissionControl) {
            throw new AlbumException(AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_PLAN);
        }
    }

    private void validatePaidStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new AlbumException(PaymentErrorCode.NOT_PAID);
        }
    }

    private void validatePaymentMemberMismatch(Payment payment, Member member) {
        if (!payment.getMember().getId().equals(member.getId())) {
            throw new AlbumException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH);
        }
    }

    private void validatePaymentNotUsed(Payment payment) {
        if (payment.getAlbum() != null) {
            throw new AlbumException(PaymentErrorCode.ALREADY_USED_PAYMENT);
        }
    }

    private Payment getPaidPaymentById(Long paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new AlbumException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateInvitationCode(InvitationCode currentInvitationCode, String code) {
        if (!currentInvitationCode.getCode().equals(code)) {
            throw new AlbumException(AlbumErrorCode.INVITATION_CODE_MISMATCH);
        }
    }

    private void validateAlbumRejoin(Member member, Album album) {
        participantRepository
                .findByMemberIdAndAlbumId(member.getId(), album.getId())
                .ifPresent(
                        p -> {
                            throw new AlbumException(AlbumErrorCode.ALREADY_PARTICIPATED);
                        });
    }

    private void validateRemainingParticipants(Long albumId, Long memberId) {
        if (participantRepository.existsByAlbumIdAndMemberIdIsNot(albumId, memberId)) {
            throw new AlbumException(AlbumErrorCode.OTHER_PARTICIPANTS_EXIST);
        }
    }
}
