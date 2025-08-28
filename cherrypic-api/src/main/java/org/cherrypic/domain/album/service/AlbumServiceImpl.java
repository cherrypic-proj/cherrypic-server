package org.cherrypic.domain.album.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.event.AlbumDeleteNotificationSendEvent;
import org.cherrypic.domain.album.event.AlbumImagesDeleteEvent;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.event.ImageDeleteEvent;
import org.cherrypic.domain.image.event.ImagesDeleteEvent;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.springframework.context.ApplicationEventPublisher;
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
    private final EventRepository eventRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public AlbumCreateResponse createAlbum(AlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validatePaymentRequirementForType(request.type(), request.paymentId());

        validatePermissionControl(request.type(), request.permissionControl());

        Album album =
                Album.createAlbum(
                        request.title(),
                        request.coverUrl(),
                        request.type(),
                        request.permissionControl());

        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.HOST);
        participant.assignFavorites();

        album.addParticipant(participant);

        if (request.type() != AlbumType.BASIC) {
            final Payment payment = getPaidPaymentById(request.paymentId());

            validatePaymentMemberMismatch(payment, currentMember);

            payment.updatePayment(PaymentPurpose.CREATION, album);

            subscriptionRepository.save(
                    Subscription.createSubscription(currentMember, album, payment.getPaidAt()));
        }

        albumRepository.save(album);

        return AlbumCreateResponse.from(album);
    }

    @Override
    public AlbumUpdateResponse updateAlbum(Long albumId, AlbumUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());

        if (album.getCoverUrl() != null && !album.getCoverUrl().equals(request.coverUrl())) {
            eventPublisher.publishEvent(ImageDeleteEvent.of(album.getCoverUrl()));
        }

        album.updateAlbum(request.title(), request.coverUrl());

        return AlbumUpdateResponse.from(album);
    }

    @Override
    public PermissionToggleResponse togglePermission(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validatePermissionControl(album.getType(), true);

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
    public AlbumJoinResponse joinAlbum(Long albumId, String code) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumByIdWithLock(albumId);
        final InvitationCode currentInvitationCode =
                invitationCodeRepository
                        .findById(album.getId())
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                AlbumErrorCode.INVITATION_CODE_NOT_FOUND));

        validateAlbumRejoin(currentMember, album);
        validateInvitationCode(currentInvitationCode, code);
        validateMaxParticipantLimit(album);

        Participant participant =
                Participant.createParticipant(currentMember, album, ParticipantRole.STANDARD);
        participant.assignFavorites();
        participantRepository.save(participant);

        return AlbumJoinResponse.from(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumInfoResponse getAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());
        String hostName = getAlbumHostByAlbumId(album.getId()).getMember().getNickname();
        int numOfParticipants = participantRepository.countByAlbumId(album.getId());

        return AlbumInfoResponse.of(
                album.getTitle(),
                album.getCoverUrl(),
                album.getType(),
                album.getCapacityGb(),
                album.getType().getCapacityGb(),
                hostName,
                numOfParticipants);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<AlbumListResponse> getParticipatingAlbumsByCondition(
            AlbumType type, String keyword, Long lastAlbumId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();

        Slice<AlbumListResponse> results =
                albumRepository.findAllByMemberIdAndTypeAndKeyword(
                        currentMember.getId(), type, keyword, lastAlbumId, size, direction);

        return SliceResponse.from(results);
    }

    @Override
    public void deleteAlbum(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateAlbumHost(currentMember.getId(), album.getId());
        validateSubscriptionInactive(album);
        validateRemainingParticipants(album, currentMember);

        final List<Event> events = eventRepository.findAllByAlbumId(album.getId());
        eventPublisher.publishEvent(
                ImagesDeleteEvent.of(events.stream().map(Event::getCoverUrl).toList()));

        eventPublisher.publishEvent(AlbumImagesDeleteEvent.of(album.getId()));
        if (album.getCoverUrl() != null) {
            eventPublisher.publishEvent(ImageDeleteEvent.of(album.getCoverUrl()));
        }

        albumRepository.delete(album);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Album getAlbumByIdWithLock(Long albumId) {
        return albumRepository
                .findByIdWithPessimisticLock(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private Participant getAlbumHostByAlbumId(Long albumId) {
        return participantRepository
                .findHostByAlbumId(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_HOST_NOT_FOUND));
    }

    private void validateAlbumHost(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (!participant.getRole().equals(ParticipantRole.HOST)) {
            throw new CustomException(AlbumErrorCode.NOT_ALBUM_HOST);
        }
    }

    private void validatePermissionControl(AlbumType type, Boolean permissionControl) {
        if (type == AlbumType.BASIC && permissionControl) {
            throw new CustomException(AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE);
        }
    }

    private void validatePaymentRequirementForType(AlbumType type, Long paymentId) {
        if (type.requiresPayment() && paymentId == null) {
            throw new CustomException(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_TYPE);
        }

        if (!type.requiresPayment() && paymentId != null) {
            throw new CustomException(AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_TYPE);
        }
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

    private void validateMaxParticipantLimit(Album album) {
        if (participantRepository.countByAlbumId(album.getId())
                >= album.getType().getMaxParticipants()) {
            throw new CustomException(AlbumErrorCode.ALBUM_PARTICIPANT_LIMIT_EXCEEDED);
        }
    }

    private void validateInvitationCode(InvitationCode currentInvitationCode, String code) {
        if (!currentInvitationCode.getCode().equals(code)) {
            throw new CustomException(AlbumErrorCode.INVITATION_CODE_MISMATCH);
        }
    }

    private void validateAlbumRejoin(Member member, Album album) {
        participantRepository
                .findByMemberIdAndAlbumId(member.getId(), album.getId())
                .ifPresent(
                        p -> {
                            throw new CustomException(AlbumErrorCode.ALREADY_PARTICIPATED);
                        });
    }

    private void validateRemainingParticipants(Album album, Member member) {
        List<Long> otherMemberIds =
                participantRepository.findOtherParticipantMemberIds(album.getId(), member.getId());

        if (!otherMemberIds.isEmpty()) {
            eventPublisher.publishEvent(
                    AlbumDeleteNotificationSendEvent.of(
                            album.getId(),
                            member.getId(),
                            member.getNickname(),
                            album.getTitle(),
                            otherMemberIds));
            throw new CustomException(AlbumErrorCode.OTHER_PARTICIPANTS_EXIST);
        }
    }

    private void validateSubscriptionInactive(Album album) {
        if (album.getType() == AlbumType.BASIC) return;

        if (album.getSubscription().getStatus() == SubscriptionStatus.ACTIVE) {
            throw new CustomException(AlbumErrorCode.SUBSCRIPTION_ACTIVE);
        }
    }
}
