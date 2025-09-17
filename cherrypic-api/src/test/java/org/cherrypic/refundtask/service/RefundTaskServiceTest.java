package org.cherrypic.refundtask.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.refundtask.dto.RefundTaskDto;
import org.cherrypic.domain.refundtask.repository.RefundTaskRepository;
import org.cherrypic.domain.refundtask.service.RefundTaskService;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.refundtask.entity.RefundTask;
import org.cherrypic.refundtask.enums.RefundTaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class RefundTaskServiceTest extends IntegrationTest {

    @Autowired private RefundTaskService refundTaskService;

    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundTaskRepository refundTaskRepository;

    @MockitoBean private MemberUtil memberUtil;
    @MockitoBean private IamportClient iamportClient;

    @Nested
    class 환불_작업을_생성할_때 {

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
        void 결제ID와_결제시간이_주어지면_10분_뒤로_예약된_환불_작업을_생성한다() {
            // given
            Long paymentId = 1L;
            LocalDateTime paidAt = LocalDateTime.of(2025, 9, 1, 13, 5);

            // when
            RefundTaskDto refundTask = refundTaskService.createRefundTask(paymentId, paidAt);

            // then
            assertThat(refundTask)
                    .extracting("paymentId", "scheduledAt")
                    .containsExactly(1L, LocalDateTime.of(2025, 9, 1, 13, 15));
        }
    }

    @Nested
    class 환불_작업을_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            RefundTask refundTask1 =
                    RefundTask.createRefundTask(1L, LocalDateTime.now().plusMinutes(10));
            RefundTask refundTask2 =
                    RefundTask.createRefundTask(2L, LocalDateTime.now().plusMinutes(10));
            refundTask2.complete(LocalDateTime.now());
            RefundTask refundTask3 =
                    RefundTask.createRefundTask(3L, LocalDateTime.now().plusMinutes(10));
            refundTaskRepository.saveAll(List.of(refundTask1, refundTask2, refundTask3));
        }

        @Test
        void Pending_상태의_환불_작업들만_조회한다() {
            // when
            List<RefundTaskDto> pendingTasks = refundTaskService.findAllPending();

            // then
            assertThat(pendingTasks).hasSize(2);
            assertThat(pendingTasks)
                    .extracting(RefundTaskDto::paymentId)
                    .containsExactlyInAnyOrder(1L, 3L);
        }
    }

    @Nested
    class 환불_작업을_수행할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album = Album.createAlbum("testAlbum", "testURL", AlbumType.PRO, false);
            albumRepository.save(album);

            Payment payment1 =
                    Payment.createPayment(
                            member,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.CREATION,
                            AlbumType.PRO);
            payment1.complete("testImpUid", "testPgProvider", LocalDateTime.of(2025, 8, 1, 13, 0));
            Payment payment2 =
                    Payment.createPayment(
                            member,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.CREATION,
                            AlbumType.PRO);
            payment2.complete("testImpUid", "testPgProvider", LocalDateTime.now());
            payment2.assignToAlbum(PaymentPurpose.CREATION, album);
            paymentRepository.saveAll(List.of(payment1, payment2));

            RefundTask refundTask =
                    RefundTask.createRefundTask(
                            payment1.getId(), LocalDateTime.now().plusMinutes(10));
            refundTaskRepository.save(refundTask);
        }

        @Test
        void 결제가_PAID_상태이고_앨범이_연결되어_있지_않으면_환불된다() throws Exception {
            // given
            CancelData cancelData = new CancelData("testImpUid", true, BigDecimal.valueOf(5900));

            given(iamportClient.cancelPaymentByImpUid(refEq(cancelData))).willReturn(null);

            iamportClient.cancelPaymentByImpUid(cancelData);

            // when
            refundTaskService.refundPayment(1L);

            // then
            Payment payment = paymentRepository.findById(1L).orElseThrow();
            RefundTask refundTask = refundTaskRepository.findByPaymentId(1L).orElseThrow();

            Assertions.assertAll(
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED),
                    () -> assertThat(payment.getCanceledAt()).isNotNull(),
                    () -> assertThat(refundTask.getStatus()).isEqualTo(RefundTaskStatus.COMPLETED),
                    () -> assertThat(refundTask.getExecutedAt()).isNotNull());
        }

        @Test
        void 앨범이_연결되어_있으면_환불되지_않는다() {
            // when
            refundTaskService.refundPayment(2L);

            // then
            Payment payment = paymentRepository.findById(2L).get();

            Assertions.assertAll(
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID),
                    () -> assertThat(payment.getCanceledAt()).isNull());
        }
    }
}
