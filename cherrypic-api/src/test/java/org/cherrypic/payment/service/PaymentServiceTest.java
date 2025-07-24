package org.cherrypic.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.exception.PaymentException;
import org.cherrypic.domain.payment.service.PaymentService;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.repository.MemberRepository;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import retrofit2.HttpException;
import retrofit2.Response;

public class PaymentServiceTest extends IntegrationTest {

    @Autowired private PaymentService paymentService;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private MemberRepository memberRepository;

    @MockitoBean private IamportClient iamportClient;
    @Mock private IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse;
    @Mock private com.siot.IamportRestClient.response.Payment iamportPayment;

    private Member member;

    @BeforeEach
    void setUp() {
        member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        "testProfileImageUrl");
        memberRepository.save(member);

        UserDetails userDetails =
                User.withUsername(member.getId().toString())
                        .password("")
                        .authorities(member.getRole().name())
                        .build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Nested
    class 앨범_유료_플랜_결제를_준비할_때 {

        @Test
        void 유효한_요청이면_결제_준비_정보를_생성한다() {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumPlan.PRO);

            // when
            paymentService.preparePayment(request);

            // then
            Payment payment = paymentRepository.findById(1L).orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(payment.getId()).isEqualTo(1L),
                    () -> assertThat(payment.getMember().getId()).isEqualTo(1L),
                    () -> assertThat(payment.getMerchantUid()).startsWith("album_"),
                    () -> assertThat(payment.getMerchantUid()).contains("pro"),
                    () -> assertThat(payment.getAmount()).isEqualTo(3900),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY));
        }

        @Test
        void 앨범_플랜이_BASIC이면_예외가_발생한다() {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumPlan.BASIC);

            // when & then
            assertThatThrownBy(() -> paymentService.preparePayment(request))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage(PaymentErrorCode.UNSUPPORTED_PAYMENT_PLAN.getMessage());
        }
    }

    @Nested
    class 앨범_유료_플랜_결제를_검증할_때 {

        private static final String MERCHANT_UID_EXISTING = "album_20250724_pro_1_a5c5dd8beaa6";
        private static final String MERCHANT_UID_NON_EXISTING =
                "album_20250704_premium_5_a5c5dd8beaa6";

        @BeforeEach
        void setUp() {
            paymentRepository.save(Payment.createPayment(member, MERCHANT_UID_EXISTING, 3900));
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
                    .isInstanceOf(PaymentException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        void merchantUid에_해당하는_결제가_DB에_없으면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_NON_EXISTING, BigDecimal.valueOf(7900), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        void 결제_금액이_불일치하면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_EXISTING, BigDecimal.valueOf(7900), "PAID");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage(PaymentErrorCode.AMOUNT_MISMATCH.getMessage());
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            stubIamportPayment(MERCHANT_UID_EXISTING, BigDecimal.valueOf(3900), "READY");

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(PaymentException.class)
                    .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
        }

        @Test
        void Iamport_API_통신_장애가_발생하면_예외가_발생한다() throws IamportResponseException, IOException {
            // given
            given(iamportClient.paymentByImpUid("imp_1234"))
                    .willThrow(new IOException("network error"));

            // when & then
            assertThatThrownBy(() -> paymentService.verifyPayment("imp_1234"))
                    .isInstanceOf(PaymentException.class)
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
}
