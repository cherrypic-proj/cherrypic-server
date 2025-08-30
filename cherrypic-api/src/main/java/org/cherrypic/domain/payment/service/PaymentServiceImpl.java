package org.cherrypic.domain.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final MemberUtil memberUtil;
    private final IamportClient iamportClient;

    private final PaymentRepository paymentRepository;
    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;

    @Override
    public PaymentReadyResponse preparePayment(PaymentReadyRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        AlbumType type = request.type();
        if (type.equals(AlbumType.BASIC)) {
            throw new CustomException(PaymentErrorCode.UNSUPPORTED_PAYMENT);
        }

        int price = type.getPrice();
        String merchantUid = generateMerchantUid(currentMember.getId(), type);
        String buyerName = currentMember.getNickname();

        PaymentPurpose purpose =
                determinePaymentPurpose(currentMember.getId(), request.albumId(), type);

        Payment payment = Payment.createPayment(currentMember, merchantUid, price, purpose);
        paymentRepository.save(payment);

        return PaymentReadyResponse.of(type, price, merchantUid, buyerName, purpose);
    }

    @Override
    public PaymentVerificationResponse verifyPayment(String impUid) {
        try {
            var iamportPayment = iamportClient.paymentByImpUid(impUid).getResponse();

            String merchantUid = iamportPayment.getMerchantUid();
            String pgProvider = iamportPayment.getPgProvider();
            int amount = iamportPayment.getAmount().intValue();
            PaymentStatus status = PaymentStatus.valueOf(iamportPayment.getStatus().toUpperCase());
            LocalDateTime paidAt =
                    iamportPayment
                            .getPaidAt()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

            Payment payment =
                    paymentRepository
                            .findByMerchantUid(merchantUid)
                            .orElseThrow(
                                    () -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            if (amount != payment.getAmount()) {
                throw new CustomException(PaymentErrorCode.AMOUNT_MISMATCH);
            }

            if (status != PaymentStatus.PAID) {
                throw new CustomException(PaymentErrorCode.NOT_PAID);
            }

            payment.updatePayment(impUid, pgProvider, PaymentStatus.PAID, paidAt);

            return PaymentVerificationResponse.from(payment);

        } catch (IamportResponseException e) {
            throw new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        } catch (IOException e) {
            throw new CustomException(PaymentErrorCode.IAMPORT_API_UNAVAILABLE);
        }
    }

    private String generateMerchantUid(Long memberId, AlbumType type) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        return String.format("album_%s_%s_%d_%s", date, type.name().toLowerCase(), memberId, uuid);
    }

    private PaymentPurpose determinePaymentPurpose(Long memberId, Long albumId, AlbumType type) {
        if (albumId == null) {
            return PaymentPurpose.CREATION;
        }

        final Album album = getAlbumById(albumId);
        validateAlbumHost(memberId, album.getId());

        AlbumType currentType = album.getType();

        if (currentType == type) {
            return PaymentPurpose.RENEWAL;
        }

        if (type.getPrice() > currentType.getPrice()) {
            return PaymentPurpose.UPGRADE;
        }

        throw new CustomException(PaymentErrorCode.DOWNGRADE_NOT_ALLOWED);
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
}
