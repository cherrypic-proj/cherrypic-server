package org.cherrypic.album.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.cherrypic.IntegrationTest;
import org.cherrypic.RedisCleaner;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.AlbumInfoResponse;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.event.AlbumDeleteNotificationSendEvent;
import org.cherrypic.domain.album.event.AlbumImagesDeleteEvent;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.favorites.repository.FavoritesRepository;
import org.cherrypic.domain.image.event.ImageDeleteEvent;
import org.cherrypic.domain.image.event.ImagesDeleteEvent;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.exception.PaymentDomainErrorCode;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.ReflectionTestUtils;

@RecordApplicationEvents
class AlbumServiceTest extends IntegrationTest {

    @Autowired private RedisCleaner redisCleaner;
    @Autowired private TransactionUtil transactionUtil;

    @Autowired private AlbumService albumService;
    @Autowired private ApplicationEvents applicationEvents;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private FavoritesRepository favoritesRepository;
    @Autowired private InvitationCodeRepository invitationCodeRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ImageRepository imageRepository;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 앨범을_생성할_때 {

        @Nested
        class BASIC_유형인_경우 {

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
            void 결제ID_없이_요청하면_앨범과_HOST_참여자가_생성된다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, null, false);

                // when
                albumService.createAlbum(request);

                // then
                Album album =
                        transactionUtil.getResult(
                                () -> {
                                    Album loadedAlbum = albumRepository.findById(1L).get();
                                    loadedAlbum.getParticipants().get(0);
                                    return loadedAlbum;
                                });
                Participant participant = album.getParticipants().get(0);

                Assertions.assertAll(
                        () ->
                                assertThat(album)
                                        .extracting(
                                                "id",
                                                "title",
                                                "coverUrl",
                                                "type",
                                                "permissionControl")
                                        .containsExactly(
                                                1L,
                                                "testTitle",
                                                "testCoverUrl",
                                                AlbumType.BASIC,
                                                false),
                        () ->
                                assertThat(participant)
                                        .extracting("id", "member.id", "album.id", "role")
                                        .containsExactly(1L, 1L, 1L, ParticipantRole.HOST));
            }

            @Test
            void 결제ID를_포함하여_요청하면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, 1L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(
                                AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_TYPE.getMessage());
            }

            @Test
            void 권한_부여_활성화_여부를_true로_요청하면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, null, true);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(
                                AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE
                                        .getMessage());
            }
        }

        @Nested
        class PRO_또는_PREMIUM_유형인_경우 {

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

                Album album =
                        Album.createAlbum(
                                "testPaidTitle", "testPaidCoverUrl", AlbumType.PRO, false);
                albumRepository.save(album);

                // 검증 완료된 결제
                Payment payment1 =
                        Payment.createPayment(
                                member1,
                                "testMerchantUid",
                                5900,
                                PaymentPurpose.CREATION,
                                AlbumType.PRO);
                payment1.complete(
                        "testImpUid", "testPgProvider", LocalDateTime.of(2025, 8, 1, 13, 0));
                // 검증되지 않은 결제
                Payment payment2 =
                        Payment.createPayment(
                                member1,
                                "testMerchantUid",
                                5900,
                                PaymentPurpose.CREATION,
                                AlbumType.PRO);
                // 검증 완료 + 유료 앨범에 쓰인 결제
                Payment payment3 =
                        Payment.createPayment(
                                member1,
                                "testMerchantUid",
                                5900,
                                PaymentPurpose.CREATION,
                                AlbumType.PRO);
                payment3.complete("testImpUid", "testPgProvider", LocalDateTime.now());
                payment3.assignToAlbum(PaymentPurpose.CREATION, album);
                // 구독 갱신 목적으로 쓰인 결제
                Payment payment4 =
                        Payment.createPayment(
                                member1,
                                "testMerchantUid",
                                5900,
                                PaymentPurpose.RENEWAL,
                                AlbumType.PRO);
                payment4.complete("testImpUid", "testPgProvider", LocalDateTime.now());
                // 취소된 결제
                Payment payment5 =
                        Payment.createPayment(
                                member1,
                                "testMerchantUid",
                                5900,
                                PaymentPurpose.CREATION,
                                AlbumType.PRO);
                payment5.complete("testImpUid", "testPgProvider", LocalDateTime.now());
                payment5.cancel(LocalDateTime.now());
                paymentRepository.saveAll(
                        List.of(payment1, payment2, payment3, payment4, payment5));
            }

            @Test
            void 유효한_결제ID면_앨범과_HOST_참여자_및_구독이_생성되고_결제에_앨범이_연결된다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, true);

                // when
                albumService.createAlbum(request);

                // then
                Album album =
                        transactionUtil.getResult(
                                () -> {
                                    Album loadedAlbum = albumRepository.findById(2L).get();
                                    loadedAlbum.getParticipants().get(0);
                                    loadedAlbum.getPayments().get(0);
                                    return loadedAlbum;
                                });
                Participant participant = album.getParticipants().get(0);
                Payment payment = album.getPayments().get(0);

                Subscription subscription = subscriptionRepository.findById(1L).orElseThrow();

                Assertions.assertAll(
                        () ->
                                assertThat(album)
                                        .extracting(
                                                "id",
                                                "title",
                                                "coverUrl",
                                                "type",
                                                "permissionControl")
                                        .containsExactly(
                                                2L,
                                                "testTitle",
                                                "testCoverUrl",
                                                AlbumType.PRO,
                                                true),
                        () ->
                                assertThat(participant)
                                        .extracting("id", "member.id", "album.id", "role")
                                        .containsExactly(1L, 1L, 2L, ParticipantRole.HOST),
                        () ->
                                assertThat(payment)
                                        .extracting("album.id", "status")
                                        .containsExactly(2L, PaymentStatus.PAID),
                        () ->
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
                                                2L,
                                                SubscriptionStatus.ACTIVE,
                                                LocalDateTime.of(2025, 8, 1, 13, 0),
                                                LocalDateTime.of(2025, 9, 1, 13, 0),
                                                LocalDateTime.of(2025, 8, 29, 13, 0)));
            }

            @Test
            void 결제ID가_null이면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, null, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_TYPE.getMessage());
            }

            @Test
            void 존재하지_않는_결제ID면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 999L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
            }

            @Test
            void 결제한_회원과_로그인_회원이_일치하지_않으면_예외가_발생한다() {
                // given
                given(memberUtil.getCurrentMember())
                        .willReturn(memberRepository.findById(2L).get());

                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH.getMessage());
            }

            @Test
            void 이미_취소된_결제라면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 5L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentDomainErrorCode.ALREADY_CANCELED.getMessage());
            }

            @Test
            void 완료되지_않은_결제라면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 2L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentDomainErrorCode.NOT_PAID.getMessage());
            }

            @Test
            void 결제가_이미_사용된_경우_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 3L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentDomainErrorCode.ALREADY_USED_PAYMENT.getMessage());
            }

            @Test
            void 결제의_목적이_앨범_생성과_일치하지_않으면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 4L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(PaymentDomainErrorCode.PAYMENT_PURPOSE_MISMATCH.getMessage());
            }
        }
    }

    @Nested
    class 앨범을_수정할_때 {

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
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            Participant participant3 =
                    Participant.createParticipant(member, album4, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            Subscription subscription =
                    Subscription.createSubscription(member, album4, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
        }

        @Test
        void 유효한_요청이면_앨범이_수정된다() {
            // given
            AlbumUpdateRequest request =
                    new AlbumUpdateRequest("testUpdatedTitle", "testUpdatedCoverUrl");

            // when
            albumService.updateAlbum(1L, request);

            // then
            Album album = albumRepository.findById(1L).get();
            Assertions.assertAll(
                    () ->
                            assertThat(album)
                                    .extracting("id", "title", "coverUrl", "type")
                                    .containsExactly(
                                            1L,
                                            "testUpdatedTitle",
                                            "testUpdatedCoverUrl",
                                            AlbumType.BASIC));
        }

        @Test
        void 앨범_커버_URL이_교체되는_경우_S3에서_이미지를_삭제하는_이벤트를_발행한다() {
            // given
            AlbumUpdateRequest request =
                    new AlbumUpdateRequest("testUpdatedTitle", "testUpdatedCoverUrl");

            // when
            albumService.updateAlbum(1L, request);

            // then
            var events = applicationEvents.stream(ImageDeleteEvent.class).toList();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().imageUrl()).isEqualTo("testURL1");
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(3L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(4L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }
    }

    @Nested
    class 앨범_권한_부여_토글_상태를_변경_할_때 {

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
            Member member3 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId3", "testOauthProvider3"),
                            "testNickname3",
                            "testProfileImageUrl3");
            Member member4 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId4", "testOauthProvider4"),
                            "testNickname4",
                            "testProfileImageUrl4");
            memberRepository.saveAll(List.of(member1, member2, member3, member4));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.PRO, true);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.BASIC, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumType.PRO, true);
            albumRepository.saveAll(List.of(album1, album2, album3, album4, album5));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member1, album2, ParticipantRole.LIMITED);
            Participant participant3 =
                    Participant.createParticipant(member1, album3, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member2, album1, ParticipantRole.HOST);
            Participant participant5 =
                    Participant.createParticipant(member3, album1, ParticipantRole.STANDARD);
            Participant participant6 =
                    Participant.createParticipant(member3, album1, ParticipantRole.LIMITED);
            Participant participant7 =
                    Participant.createParticipant(member1, album5, ParticipantRole.HOST);
            participantRepository.saveAll(
                    List.of(
                            participant1,
                            participant2,
                            participant3,
                            participant4,
                            participant5,
                            participant6,
                            participant7));

            Subscription subscription1 =
                    Subscription.createSubscription(member1, album1, LocalDateTime.now());
            Subscription subscription2 =
                    Subscription.createSubscription(member1, album5, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription2, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        }

        @Test
        void 유효한_요청이면_앨범의_권한_부여_상태가_변경되고_LIMITED_참가자_권한이_STANDARD로_수정된다() {
            // when
            albumService.togglePermission(1L);

            // then
            Album album =
                    transactionUtil.getResult(
                            () -> {
                                Album loadedAlbum = albumRepository.findById(1L).get();
                                loadedAlbum.getParticipants().size();
                                return loadedAlbum;
                            });
            List<Participant> participants = album.getParticipants();

            Assertions.assertAll(
                    () -> assertThat(album.getPermissionControl()).isFalse(),
                    () ->
                            assertThat(participants)
                                    .extracting("role")
                                    .containsExactlyInAnyOrder(
                                            ParticipantRole.HOST,
                                            ParticipantRole.HOST,
                                            ParticipantRole.STANDARD,
                                            ParticipantRole.STANDARD));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(4L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(5L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE
                                    .getMessage());
        }
    }

    @Nested
    class 초대_코드를_생성할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId2", "testOauthProvider2"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album2, ParticipantRole.STANDARD);
            Participant participant3 =
                    Participant.createParticipant(member1, album3, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            Subscription subscription =
                    Subscription.createSubscription(member1, album3, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
        }

        @AfterEach
        void cleanUp() {
            redisCleaner.flushAll();
        }

        @Test
        void 유효한_요청이면_초대_코드를_저장하며_초대_링크가_반환된다() {
            // when
            InvitationLinkCreateResponse response = albumService.createInvitationLink(1L);

            // then
            InvitationCode savedCode = invitationCodeRepository.findById(1L).orElseThrow();

            String link = response.invitationLink();
            assertThat(link)
                    .containsPattern(String.format(".*albumId=%d&code=%s", 1, savedCode.getCode()));
        }

        @Test
        void 유효한_초대_코드가_이미_존재하는_경우_갱신하지_않는다() {
            // given
            InvitationCode invitationCode =
                    InvitationCode.builder().albumId(1L).code("testInvitationCode").build();
            invitationCodeRepository.save(invitationCode);
            String invitationCodeBefore = invitationCode.getCode();

            // when
            albumService.createInvitationLink(1L);

            // then
            Optional<InvitationCode> code = invitationCodeRepository.findById(1L);
            Assertions.assertAll(
                    () -> assertThat(code).isPresent(),
                    () -> assertThat(invitationCodeBefore).isEqualTo(code.get().getCode()));
        }

        @Test
        void 유효한_요청에_대해서_유효한_초대_코드가_생성된다() {
            // when
            albumService.createInvitationLink(1L);

            // then
            InvitationCode createdCode = invitationCodeRepository.findById(1L).orElseThrow();
            assertThat(createdCode)
                    .extracting("albumId", "code", "ttl")
                    .containsExactly(1L, createdCode.getCode(), 86400L);
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }
    }

    @Nested
    class 앨범에_입장할_때 {

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
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4));

            Participant participant1 =
                    Participant.createParticipant(member, album3, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album4, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2));

            InvitationCode invitationCode1 =
                    InvitationCode.builder()
                            .albumId(1L)
                            .code("testInvitationCode1")
                            .ttl(Duration.ofMinutes(30).getSeconds())
                            .build();
            InvitationCode invitationCode2 =
                    InvitationCode.builder()
                            .albumId(3L)
                            .code("testInvitationCode2")
                            .ttl(Duration.ofMinutes(30).getSeconds())
                            .build();
            InvitationCode invitationCode3 =
                    InvitationCode.builder()
                            .albumId(4L)
                            .code("testInvitationCode3")
                            .ttl(Duration.ofMinutes(30).getSeconds())
                            .build();
            invitationCodeRepository.saveAll(
                    List.of(invitationCode1, invitationCode2, invitationCode3));

            Subscription subscription =
                    Subscription.createSubscription(member, album4, LocalDateTime.now());
            ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
        }

        @AfterEach
        void cleanUp() {
            redisCleaner.flushAll();
        }

        @Test
        void 유효한_초대_코드면_앨범_참가자가_생성된다() {
            // when
            albumService.joinAlbum(1L, "testInvitationCode1");

            // then
            Participant participant =
                    participantRepository.findByMemberIdAndAlbumId(1L, 1L).orElseThrow();

            Assertions.assertAll(
                    () -> assertThat(participant).isNotNull(),
                    () -> assertThat(participant.getRole()).isEqualTo(ParticipantRole.STANDARD));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(999L, "testInvitationCode1"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_초대_코드가_redis에_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(2L, "NoneExistingCode"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.INVITATION_CODE_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_초대_코드가_redis에_저장된_코드와_일치하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(1L, "expiredInvitationCode"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.INVITATION_CODE_MISMATCH.getMessage());
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(4L, "testInvitationCode3"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.EXPIRED_SUBSCRIPTION.getMessage());
        }

        @Test
        void 이미_입장한_앨범에_재입장_하려는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(3L, "testInvitationCode2"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALREADY_PARTICIPATED.getMessage());
        }

        @Test
        void 최대_참가자_수를_초과하면_예외가_발생한다() {
            // given
            Album album = albumRepository.findById(1L).orElseThrow();
            int maxParticipants = album.getType().getMaxParticipants();

            for (int i = 0; i < maxParticipants; i++) {
                Member member =
                        Member.createMember(
                                OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                                "testNickname",
                                "testProfileImageUrl");
                memberRepository.save(member);

                Participant participant =
                        Participant.createParticipant(member, album, ParticipantRole.STANDARD);
                participantRepository.save(participant);
            }

            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(album.getId(), "testInvitationCode1"))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_PARTICIPANT_LIMIT_EXCEEDED.getMessage());
        }
    }

    @Nested
    class 개별_앨범을_조회할_때 {

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
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.STANDARD);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 유효한_요청인_경우_앨범_정보를_반환한다() {
            // when
            AlbumInfoResponse response = albumService.getAlbum(1L);

            // then
            assertThat(response)
                    .extracting(
                            "title",
                            "coverUrl",
                            "type",
                            "capacityUsed",
                            "totalCapacity",
                            "hostName",
                            "numOfParticipants")
                    .containsExactly(
                            "testAlbum1",
                            "testURL1",
                            AlbumType.BASIC,
                            new BigDecimal("0.00"),
                            new BigDecimal("3"),
                            "testNickname",
                            1);
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.getAlbum(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참여자가_아닌_경우_에외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.getAlbum(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범에_방장이_없는_경우_에외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.getAlbum(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_HOST_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 앨범_목록을_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumType.BASIC, false);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testTitle3", "testCoverUrl3", AlbumType.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.HOST);
            Participant participant3 =
                    Participant.createParticipant(member, album3, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            Favorites favorites1 = Favorites.createFavorites(participant1);
            Favorites favorites2 = Favorites.createFavorites(participant2);
            Favorites favorites3 = Favorites.createFavorites(participant3);
            favoritesRepository.saveAll(List.of(favorites1, favorites2, favorites3));
        }

        @Test
        void PRO_유형으로_필터링하면_PRO_앨범만_조회한다() {
            // given
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            AlbumType.PRO, null, null, 1, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().get(0).albumId()).isEqualTo(3),
                    () -> assertThat(response.content().get(0).type()).isEqualTo(AlbumType.PRO),
                    () ->
                            assertThat(response.content().get(0).price())
                                    .isEqualTo(AlbumType.PRO.getPrice()),
                    () ->
                            assertThat(
                                            response.content()
                                                    .get(0)
                                                    .createdAt()
                                                    .truncatedTo(ChronoUnit.MINUTES))
                                    .isEqualTo(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 앨범_이름으로_필터링하면_일치하는_앨범만_조회한다() {
            // given
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, "title2", null, 1, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().get(0).albumId()).isEqualTo(2),
                    () -> assertThat(response.content().get(0).title()).isEqualTo("testTitle2"),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void PRO_유형과_앨범_이름으로_필터링하면_조건을_모두_만족하는_앨범만_조회한다() {
            // given
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            AlbumType.PRO, "title3", null, 1, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().get(0).albumId()).isEqualTo(3),
                    () -> assertThat(response.content().get(0).title()).isEqualTo("testTitle3"),
                    () -> assertThat(response.content().get(0).type()).isEqualTo(AlbumType.PRO),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 정렬_조건이_ASC이면_albumId를_오름차순으로_조회한다() {
            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, null, null, 3, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("albumId")
                                    .containsExactly(1L, 2L, 3L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 정렬_조건이_DESC이면_albumId를_내림차순으로_조회한다() {
            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, null, null, 3, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("albumId")
                                    .containsExactly(3L, 2L, 1L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, null, null, 3, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(3),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_반환한다() {
            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, null, null, 1, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(1),
                    () -> assertThat(response.isLast()).isFalse());
        }

        @Test
        void 앨범이_없는_경우_빈_리스트를_조회한다() {
            // given
            albumRepository.deleteAll();

            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbumsByCondition(
                            null, null, null, 10, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }
    }

    @Nested
    class 앨범을_삭제할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumType.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumType.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumType.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumType.BASIC, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumType.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4, album5));

            Image image =
                    Image.createImage(album1, 1L, "testUrl", LocalDateTime.now(), BigDecimal.ONE);
            imageRepository.save(image);

            subscriptionRepository.save(
                    Subscription.createSubscription(member1, album5, LocalDateTime.now()));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member1, album2, ParticipantRole.LIMITED);
            Participant participant3 =
                    Participant.createParticipant(member1, album3, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member2, album3, ParticipantRole.LIMITED);
            Participant participant5 =
                    Participant.createParticipant(member1, album5, ParticipantRole.HOST);
            participantRepository.saveAll(
                    List.of(participant1, participant2, participant3, participant4, participant5));

            eventRepository.save(Event.createEvent(album1, "testTitle1", "testCoverUrl1"));
        }

        @Test
        void 유효한_요청일_경우_앨범과_내부_이벤트가_모두_삭제된다() {
            // when
            albumService.deleteAlbum(1L);

            // then
            Assertions.assertAll(
                    () -> assertThat(albumRepository.findById(1L).isPresent()).isFalse(),
                    () -> assertThat(eventRepository.findById(1L).isPresent()).isFalse());
        }

        @Test
        void 유효한_요청일_경우_S3에_존재하는_앨범_이미지들을_삭제하는_이벤트를_발행한다() {
            // when
            albumService.deleteAlbum(1L);

            // then
            var events = applicationEvents.stream(AlbumImagesDeleteEvent.class).toList();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().albumId()).isEqualTo(1L);
        }

        @Test
        void 유효한_요청일_경우_S3에_존재하는_앨범_커버를_삭제하는_이벤트를_발행한다() {
            // when
            albumService.deleteAlbum(1L);

            // then
            var events = applicationEvents.stream(ImageDeleteEvent.class).toList();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().imageUrl()).isEqualTo("testURL1");
        }

        @Test
        void 유효한_요청일_경우_S3에_존재하는_이벤트_커버들을_삭제하는_이벤트를_발행한다() {
            // when
            albumService.deleteAlbum(1L);

            // then
            var events = applicationEvents.stream(ImagesDeleteEvent.class).toList();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().imageUrls()).isEqualTo(List.of("testCoverUrl1"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(4L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 다른_참가자가_남아있는_경우_이벤트가_발행되고_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.OTHER_PARTICIPANTS_EXIST.getMessage());

            var events = applicationEvents.stream(AlbumDeleteNotificationSendEvent.class).toList();
            Assertions.assertAll(
                    () -> assertThat(events).hasSize(1),
                    () -> assertThat(events.getFirst().albumId()).isEqualTo(3L));
        }

        @Test
        void 구독_중인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(5L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.SUBSCRIPTION_ACTIVE.getMessage());
        }
    }
}
