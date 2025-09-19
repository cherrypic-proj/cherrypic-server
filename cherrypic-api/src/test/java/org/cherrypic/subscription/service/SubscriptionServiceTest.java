package org.cherrypic.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.refundtask.repository.RefundTaskRepository;
import org.cherrypic.domain.subscription.dto.request.SubscriptionRenewRequest;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.exception.PaymentDomainErrorCode;
import org.cherrypic.refundtask.entity.RefundTask;
import org.cherrypic.refundtask.enums.RefundTaskStatus;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.cherrypic.subscription.exception.SubscriptionDomainErrorCode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

class SubscriptionServiceTest extends IntegrationTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RefundTaskRepository refundTaskRepository;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 구독을_해지할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.PRO, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.PRO, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.PREMIUM, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumType.PREMIUM, false);
            Album album6 = Album.createAlbum("testAlbum6", "testURL6", AlbumType.PREMIUM, false);
            Album album7 = Album.createAlbum("testAlbum7", "testURL7", AlbumType.PRO, false);
            albumRepository.saveAll(
                    List.of(album1, album2, album3, album4, album5, album6, album7));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.HOST);
            Participant participant3 =
                    Participant.createParticipant(member, album3, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member, album4, ParticipantRole.STANDARD);
            Participant participant5 =
                    Participant.createParticipant(member, album5, ParticipantRole.HOST);
            Participant participant6 =
                    Participant.createParticipant(member, album7, ParticipantRole.HOST);
            participantRepository.saveAll(
                    List.of(
                            participant1,
                            participant2,
                            participant3,
                            participant4,
                            participant5,
                            participant6));

            Subscription subscription1 =
                    Subscription.createSubscription(member, album2, LocalDateTime.now());
            Subscription subscription2 =
                    Subscription.createSubscription(
                            member, album3, LocalDateTime.of(2025, 1, 1, 0, 0));
            Subscription subscription3 =
                    Subscription.createSubscription(member, album7, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription3, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.saveAll(List.of(subscription1, subscription2, subscription3));
        }

        @Test
        void 유효한_요청이면_구독을_해지한다() {
            // when
            subscriptionService.cancelSubscription(2L);

            // then
            Subscription subscription = subscriptionRepository.findByAlbumId(2L).get();
            Assertions.assertAll(
                    () ->
                            assertThat(subscription)
                                    .extracting("id", "status")
                                    .containsExactly(1L, SubscriptionStatus.CANCELED));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(6L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(4L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(7L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE
                                    .getMessage());
        }

        @Test
        void 이미_해지된_구독이면_예외가_발생한다() {
            // given
            subscriptionService.cancelSubscription(2L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionDomainErrorCode.ALREADY_CANCELED.getMessage());
        }

        @Test
        void 만료된_구독이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionDomainErrorCode.ALREADY_EXPIRED.getMessage());
        }
    }

    @Nested
    class 구독을_갱신할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId1", "testOauthProvider1"),
                            "testNickname1",
                            "testProfileImageUrl1");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId2", "testOauthProvider2"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.PRO, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.PRO, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.PRO, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.BASIC, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumType.PREMIUM, false);
            Album album6 = Album.createAlbum("testAlbum6", "testURL6", AlbumType.PREMIUM, false);
            Album album7 = Album.createAlbum("testAlbum7", "testURL7", AlbumType.PREMIUM, false);
            Album album8 = Album.createAlbum("testAlbum8", "testURL8", AlbumType.PRO, false);
            albumRepository.saveAll(
                    List.of(album1, album2, album3, album4, album5, album6, album7, album8));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member1, album3, ParticipantRole.STANDARD);
            Participant participant3 =
                    Participant.createParticipant(member1, album4, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member1, album5, ParticipantRole.HOST);
            Participant participant5 =
                    Participant.createParticipant(member1, album6, ParticipantRole.HOST);
            Participant participant6 =
                    Participant.createParticipant(member1, album7, ParticipantRole.HOST);
            Participant participant7 =
                    Participant.createParticipant(member1, album8, ParticipantRole.HOST);
            participantRepository.saveAll(
                    List.of(
                            participant1,
                            participant2,
                            participant3,
                            participant4,
                            participant5,
                            participant6,
                            participant7));

            // 9월 1일에 시작되어 현재는 해지된 구독
            Subscription subscription1 =
                    Subscription.createSubscription(
                            member1, album1, LocalDateTime.of(2025, 9, 1, 0, 0));
            subscription1.cancel();
            // 종료된 구독
            Subscription subscription2 =
                    Subscription.createSubscription(
                            member1, album7, LocalDateTime.of(2025, 7, 1, 0, 0));
            // 구독 중
            Subscription subscription3 =
                    Subscription.createSubscription(member1, album6, LocalDateTime.now());
            // 만료된 구독
            Subscription subscription4 =
                    Subscription.createSubscription(member1, album8, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription4, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.saveAll(
                    List.of(subscription1, subscription2, subscription3, subscription4));

            // 완료된 결제
            Payment payment1 =
                    Payment.createPayment(
                            member1,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.RENEWAL,
                            AlbumType.PRO);
            payment1.complete("testImpUid", "testPgProvider", LocalDateTime.now().minusDays(15));

            // 완료된 다른 회원의 결제
            Payment payment2 =
                    Payment.createPayment(
                            member2,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.RENEWAL,
                            AlbumType.PRO);
            payment2.complete("testImpUid", "testPgProvider", LocalDateTime.now());

            // 완료되지 않은 결제
            Payment payment3 =
                    Payment.createPayment(
                            member1,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.RENEWAL,
                            AlbumType.PRO);

            // 유료 앨범 생성에 쓰인 완료된 결제
            Payment payment4 =
                    Payment.createPayment(
                            member1,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.CREATION,
                            AlbumType.PRO);
            payment4.complete("testImpUid", "testPgProvider", LocalDateTime.now());
            payment4.assignToAlbum(PaymentPurpose.CREATION, album1);

            // 구독 업그레이드 목적으로 쓰인 결제
            Payment payment5 =
                    Payment.createPayment(
                            member1,
                            "testMerchantUid",
                            12900,
                            PaymentPurpose.UPGRADE,
                            AlbumType.PREMIUM);
            payment5.complete("testImpUid", "testPgProvider", LocalDateTime.now());

            // 취소된 결제
            Payment payment6 =
                    Payment.createPayment(
                            member1,
                            "testMerchantUid",
                            5900,
                            PaymentPurpose.RENEWAL,
                            AlbumType.PRO);
            payment6.complete("testImpUid", "testPgProvider", LocalDateTime.now());
            payment6.cancel(LocalDateTime.now());

            paymentRepository.saveAll(
                    List.of(payment1, payment2, payment3, payment4, payment5, payment6));

            refundTaskRepository.save(
                    RefundTask.createRefundTask(1L, LocalDateTime.now().plusMinutes(10)));
        }

        @Test
        void 유효한_요청이면_구독을_갱신하고_결제에_앨범이_연결된다() {
            // given
            Subscription canceledSubscription = subscriptionRepository.findById(1L).get();
            LocalDateTime previousEndAt =
                    canceledSubscription.getEndAt().truncatedTo(ChronoUnit.MINUTES);

            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when
            subscriptionService.renewSubscription(1L, request);

            // then
            Payment payment = paymentRepository.findTop1ByAlbumIdOrderByIdAsc(1L).get();
            Subscription subscription = subscriptionRepository.findByAlbumId(1L).get();

            Assertions.assertAll(
                    () ->
                            assertThat(payment)
                                    .extracting("album.id", "status")
                                    .containsExactly(1L, PaymentStatus.PAID),
                    () ->
                            assertThat(subscription)
                                    .extracting("id", "member.id", "album.id", "status")
                                    .containsExactly(1L, 1L, 1L, SubscriptionStatus.ACTIVE),
                    () ->
                            assertThat(subscription.getEndAt().truncatedTo(ChronoUnit.MINUTES))
                                    .isEqualTo(previousEndAt.plusMonths(1)),
                    () ->
                            assertThat(
                                            subscription
                                                    .getNextBillingAt()
                                                    .truncatedTo(ChronoUnit.MINUTES))
                                    .isEqualTo(previousEndAt.plusMonths(1).minusDays(3)));
        }

        @Test
        void 결제에_앨범이_연결되면_환불_예약_작업이_SKIP된다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when
            subscriptionService.renewSubscription(1L, request);

            // then
            RefundTask refundTask = refundTaskRepository.findByPaymentId(1L).get();
            assertThat(refundTask)
                    .extracting("id", "paymentId", "status")
                    .containsExactly(1L, 1L, RefundTaskStatus.SKIPPED);
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(3L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(8L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(4L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE
                                    .getMessage());
        }

        @Test
        void 결제가_존재하지_않는_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(999L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }

        @Test
        void 결제한_회원과_구독을_갱신하려는_회원이_일치하지_않으면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(2L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH.getMessage());
        }

        @Test
        void 이미_취소된_결제라면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(6L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentDomainErrorCode.ALREADY_CANCELED.getMessage());
        }

        @Test
        void 완료되지_않은_결제라면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(3L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentDomainErrorCode.NOT_PAID.getMessage());
        }

        @Test
        void 결제가_이미_사용된_경우_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(4L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentDomainErrorCode.ALREADY_USED_PAYMENT.getMessage());
        }

        @Test
        void 결제의_목적이_구독_갱신과_일치하지_않으면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(5L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(PaymentDomainErrorCode.PAYMENT_PURPOSE_MISMATCH.getMessage());
        }

        @Test
        void 이미_구독_중인_상태면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(6L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionDomainErrorCode.ALREADY_ACTIVE.getMessage());
        }

        @Test
        void 만료된_구독이면_예외가_발생한다() {
            // given
            SubscriptionRenewRequest request = new SubscriptionRenewRequest(1L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.renewSubscription(7L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionDomainErrorCode.ALREADY_EXPIRED.getMessage());
        }
    }

    @Nested
    class 구독_정보를_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId1", "testOauthProvider1"),
                            "testNickname1",
                            "testProfileImageUrl1");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId2", "testOauthProvider2"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.PRO, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.PRO, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.PRO, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.BASIC, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumType.PREMIUM, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4, album5));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member1, album3, ParticipantRole.STANDARD);
            Participant participant3 =
                    Participant.createParticipant(member1, album4, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member1, album5, ParticipantRole.HOST);
            participantRepository.saveAll(
                    List.of(participant1, participant2, participant3, participant4));

            Subscription subscription =
                    Subscription.createSubscription(
                            member1, album1, LocalDateTime.of(2025, 8, 4, 0, 0));
            subscriptionRepository.save(subscription);
        }

        @Test
        void 유효한_요청이면_구독_정보를_조회한다() {
            // when
            subscriptionService.getSubscriptionInfo(1L);

            // then
            Subscription subscription = subscriptionRepository.findByAlbumId(1L).get();

            assertThat(subscription)
                    .extracting(
                            "id",
                            "member.id",
                            "album.id",
                            "status",
                            "startAt",
                            "endAt",
                            "nextBillingAt")
                    .containsExactly(
                            1L,
                            1L,
                            1L,
                            SubscriptionStatus.ACTIVE,
                            LocalDateTime.of(2025, 8, 4, 0, 0),
                            LocalDateTime.of(2025, 9, 4, 0, 0),
                            LocalDateTime.of(2025, 9, 1, 0, 0));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.getSubscriptionInfo(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.getSubscriptionInfo(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.getSubscriptionInfo(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.getSubscriptionInfo(4L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE
                                    .getMessage());
        }

        @Test
        void 구독이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.getSubscriptionInfo(5L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND.getMessage());
        }
    }
}
