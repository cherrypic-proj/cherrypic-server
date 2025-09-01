package org.cherrypic.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.payment.service.PaymentService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import retrofit2.HttpException;
import retrofit2.Response;

public class PaymentServiceTest extends IntegrationTest {

    @Autowired private PaymentService paymentService;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ParticipantRepository participantRepository;

    @MockitoBean private MemberUtil memberUtil;

    @MockitoBean private IamportClient iamportClient;
    @Mock private IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock private com.siot.IamportRestClient.response.Payment iamportPayment;

    @Nested
    class 결제를_준비할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album proAlbum = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumType.PRO, false);
            Album premiumAlbum =
                    Album.createAlbum("testTitle2", "testCoverUrl2", AlbumType.PREMIUM, false);
            Album basicAlbum1 =
                    Album.createAlbum("testTitle3", "testCoverUrl3", AlbumType.BASIC, false);
            Album basicAlbum2 =
                    Album.createAlbum("testTitle4", "testCoverUrl4", AlbumType.BASIC, false);
            albumRepository.saveAll(List.of(proAlbum, premiumAlbum, basicAlbum1, basicAlbum2));

            Participant participant1 =
                    Participant.createParticipant(member, proAlbum, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, premiumAlbum, ParticipantRole.HOST);
            Participant participant3 =
                    Participant.createParticipant(member, basicAlbum2, ParticipantRole.STANDARD);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));
        }

        @Nested
        class 유료_앨범_생성의_경우 {

            @Test
            void CREATION_목적의_결제_준비_정보를_생성한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, null);

                // when
                paymentService.preparePayment(request);

                // then
                Payment payment = paymentRepository.findById(1L).orElseThrow();
                Assertions.assertAll(
                        () -> assertThat(payment.getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMember().getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMerchantUid()).startsWith("album_"),
                        () -> assertThat(payment.getMerchantUid()).contains("pro"),
                        () -> assertThat(payment.getAmount()).isEqualTo(5900),
                        () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY),
                        () -> assertThat(payment.getPurpose()).isEqualTo(PaymentPurpose.CREATION));
            }
        }

        @Nested
        class 구독_갱신의_경우 {

            @Test
            void RENEWAL_목적의_결제_준비_정보를_생성한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                // when
                paymentService.preparePayment(request);

                // then
                Payment payment = paymentRepository.findById(1L).orElseThrow();
                Assertions.assertAll(
                        () -> assertThat(payment.getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMember().getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMerchantUid()).startsWith("album_"),
                        () -> assertThat(payment.getMerchantUid()).contains("pro"),
                        () -> assertThat(payment.getAmount()).isEqualTo(5900),
                        () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY),
                        () -> assertThat(payment.getPurpose()).isEqualTo(PaymentPurpose.RENEWAL));
            }

            @Test
            void 하위_앨범_유형으로_결제_준비를_요청하면_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 2L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentErrorCode.DOWNGRADE_NOT_ALLOWED.getMessage());
            }

            @Test
            void 앨범이_존재하지_않는_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 999L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
            }

            @Test
            void 앨범_참가자가_아닌_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 3L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
            }

            @Test
            void 앨범_방장이_아닌_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 4L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
            }
        }

        @Nested
        class 구독_업그레이드의_경우 {

            @Test
            void UPGRADE_목적의_결제_준비_정보를_생성한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PREMIUM, 1L);

                // when
                paymentService.preparePayment(request);

                // then
                Payment payment = paymentRepository.findById(1L).orElseThrow();
                Assertions.assertAll(
                        () -> assertThat(payment.getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMember().getId()).isEqualTo(1L),
                        () -> assertThat(payment.getMerchantUid()).startsWith("album_"),
                        () -> assertThat(payment.getMerchantUid()).contains("premium"),
                        () -> assertThat(payment.getAmount()).isEqualTo(12900),
                        () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY),
                        () -> assertThat(payment.getPurpose()).isEqualTo(PaymentPurpose.UPGRADE));
            }

            @Test
            void 하위_앨범_유형으로_결제_준비를_요청하면_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 2L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentErrorCode.DOWNGRADE_NOT_ALLOWED.getMessage());
            }

            @Test
            void 앨범이_존재하지_않는_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PREMIUM, 999L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
            }

            @Test
            void 앨범_참가자가_아닌_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PREMIUM, 3L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
            }

            @Test
            void 앨범_방장이_아닌_경우_예외가_발생한다() {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PREMIUM, 4L);

                // when & then
                assertThatThrownBy(() -> paymentService.preparePayment(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
            }
        }

        @Test
        void BASIC_유형으로_결제_준비를_요청하면_예외가_발생한다() {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.BASIC, null);

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.UNSUPPORTED_PAYMENT.getMessage());
        }

        @Test
        void 사용되지_않은_완료된_결제가_존재하면_예외가_발생한다() {
            // given
            Member member = memberRepository.findById(1L).get();

            Payment payment =
                    Payment.createPayment(
                            member, "testMerchantUid", 5900, PaymentPurpose.RENEWAL, AlbumType.PRO);
            payment.updatePayment(
                    "testImpUid", "kakaopay", PaymentStatus.PAID, LocalDateTime.now());
            paymentRepository.save(payment);

            PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, null);

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.UNLINKED_PAYMENT_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    class 결제를_검증할_때 {

        private static final String MERCHANT_UID_EXISTING = "album_20250724_pro_1_a5c5dd8beaa6";
        private static final String MERCHANT_UID_NON_EXISTING =
                "album_20250704_premium_5_a5c5dd8beaa6";

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            paymentRepository.save(
                    Payment.createPayment(
                            member,
                            MERCHANT_UID_EXISTING,
                            3900,
                            PaymentPurpose.CREATION,
                            AlbumType.PRO));
        }

        @Test
        void 유효한_요청이면_결제_정보를_검증한_후_갱신한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_EXISTING, BigDecimal.valueOf(3900), "PAID");

            // when
            paymentService.verifyPayment("imp_1234");

            // then
            Payment payment = paymentRepository.findById(1L).orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(payment.getId()).isEqualTo(1L),
                    () -> assertThat(payment.getImpUid()).isEqualTo("imp_1234"),
                    () -> assertThat(payment.getAmount()).isEqualTo(3900),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () ->
                            assertThat(payment.getPaidAt().truncatedTo(ChronoUnit.MINUTES))
                                    .isEqualTo(
                                            LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
        }

        @Test
        void impUid가_아임포트에_존재하지_않으면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            HttpException httpException =
                    new HttpException(
                            Response.error(
                                    404,
                                    ResponseBody.create(
                                            MediaType.parse("application/json"),
                                            "{\"message\":\"not found\"}")));

            given(iamportClient.paymentByImpUid("imp_1234"))
                    .willThrow(new IamportResponseException("not found", httpException));

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        void merchantUid에_해당하는_결제가_DB에_없으면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_NON_EXISTING, BigDecimal.valueOf(7900), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        void 결제_금액이_불일치하면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_EXISTING, BigDecimal.valueOf(7900), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.AMOUNT_MISMATCH.getMessage());
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_EXISTING, BigDecimal.valueOf(3900), "READY");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }

        @Test
        void Iamport_API_통신_장애가_발생하면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            given(iamportClient.paymentByImpUid("imp_1234"))
                    .willThrow(new IOException("network error"));

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.IAMPORT_API_UNAVAILABLE.getMessage());
        }

        private void stubIamportPayment(String merchantUid, BigDecimal amount, String status)
                throws IOException, IamportResponseException {
            given(iamportClient.paymentByImpUid(anyString())).willReturn(iamportResponse);
            given(iamportResponse.getResponse()).willReturn(iamportPayment);

            given(iamportPayment.getMerchantUid()).willReturn(merchantUid);
            given(iamportPayment.getPgProvider()).willReturn("kakaopay");
            given(iamportPayment.getAmount()).willReturn(amount);
            given(iamportPayment.getStatus()).willReturn(status);
            given(iamportPayment.getPaidAt()).willReturn(new Date());
        }
    }

    @Nested
    class 앨범과_연결되지_않은_완료된_결제_내역을_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);
        }

        @Test
        void 존재하면_1건의_결제_내역을_반환한다() {
            // given
            Member member = memberRepository.findById(1L).get();

            // 결제는 완료되었지만 아직 앨범과 연결되지 않은 유료 앨범 최초 생성 결제
            Payment payment =
                    Payment.createPayment(
                            member,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.CREATION,
                            AlbumType.PRO);
            payment.updatePayment(
                    "testImpUid",
                    "kakaopay",
                    PaymentStatus.PAID,
                    LocalDateTime.of(2025, 8, 31, 20, 0));
            paymentRepository.save(payment);

            PaymentUnlinkedResponse response = paymentService.getUnlinkedPayment();

            // when & then
            Assertions.assertAll(
                    () ->
                            assertThat(response)
                                    .extracting(
                                            "paymentId", "albumType", "amount", "purpose", "paidAt")
                                    .containsExactly(
                                            1L,
                                            AlbumType.PRO,
                                            5900,
                                            PaymentPurpose.CREATION,
                                            LocalDateTime.of(2025, 8, 31, 20, 0)));
        }

        @Test
        void 존재하지_않으면_null을_반환한다() {
            // given
            PaymentUnlinkedResponse response = paymentService.getUnlinkedPayment();

            // when & then
            assertThat(response).isNull();
        }
    }
}
