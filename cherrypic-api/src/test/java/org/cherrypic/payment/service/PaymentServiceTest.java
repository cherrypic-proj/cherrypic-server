package org.cherrypic.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.cherrypic.IntegrationTest;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.payment.dto.PaymentReadyRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class PaymentServiceTest extends IntegrationTest {

    @Autowired private PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        Member member =
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
}
