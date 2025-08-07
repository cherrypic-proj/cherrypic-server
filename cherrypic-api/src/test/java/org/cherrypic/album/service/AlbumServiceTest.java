package org.cherrypic.album.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.cherrypic.IntegrationTest;
import org.cherrypic.RedisCleaner;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AlbumServiceTest extends IntegrationTest {

    @Autowired private RedisCleaner redisCleaner;
    @Autowired private TransactionUtil transactionUtil;

    @Autowired private AlbumService albumService;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private InvitationCodeRepository invitationCodeRepository;
    @Autowired private EventRepository eventRepository;

    @MockitoBean MemberUtil memberUtil;

    @Nested
    class 앨범을_생성할_때 {

        @Nested
        class BASIC_플랜인_경우 {

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
                                "testTitle", "testCoverUrl", AlbumPlan.BASIC, null, false);

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
                                                "plan",
                                                "permissionControl")
                                        .containsExactly(
                                                1L,
                                                "testTitle",
                                                "testCoverUrl",
                                                AlbumPlan.BASIC,
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
                                "testTitle", "testCoverUrl", AlbumPlan.BASIC, 1L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(
                                AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN.getMessage());
            }

            @Test
            void 권한_부여_활성화_여부를_true로_요청하면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.BASIC, null, true);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(
                                AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_PLAN
                                        .getMessage());
            }
        }

        @Nested
        class PRO_또는_PREMIUM_플랜인_경우 {

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
                                "testPaidTitle", "testPaidCoverUrl", AlbumPlan.PRO, false);
                albumRepository.save(album);

                // 검증 완료된 결제
                Payment payment1 = Payment.createPayment(member1, "testMerchantUid", 3900);
                payment1.updatePayment(
                        "testImpUid", "testPgProvider", PaymentStatus.PAID, LocalDateTime.now());
                // 검증되지 않은 결제
                Payment payment2 = Payment.createPayment(member1, "testMerchantUid", 3900);
                // 검증 완료 + 유료 앨범에 쓰인 결제
                Payment payment3 = Payment.createPayment(member1, "testMerchantUid", 3900);
                payment3.updatePayment(
                        "testImpUid", "testPgProvider", PaymentStatus.PAID, LocalDateTime.now());
                payment3.updatePayment(album);
                paymentRepository.saveAll(List.of(payment1, payment2, payment3));
            }

            @Test
            void 유효한_결제ID면_앨범과_HOST_참여자_및_구독이_생성되고_결제에_앨범이_연결된다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, 1L, true);

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
                                                "plan",
                                                "permissionControl")
                                        .containsExactly(
                                                2L,
                                                "testTitle",
                                                "testCoverUrl",
                                                AlbumPlan.PRO,
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
                                        .extracting("id", "member.id", "album.id", "status")
                                        .containsExactly(1L, 1L, 2L, SubscriptionStatus.ACTIVE),
                        () ->
                                assertThat(
                                                subscription
                                                        .getStartAt()
                                                        .truncatedTo(ChronoUnit.MINUTES))
                                        .isEqualTo(
                                                LocalDateTime.now()
                                                        .truncatedTo(ChronoUnit.MINUTES)),
                        () ->
                                assertThat(subscription.getEndAt().truncatedTo(ChronoUnit.MINUTES))
                                        .isEqualTo(
                                                LocalDateTime.now()
                                                        .truncatedTo(ChronoUnit.MINUTES)
                                                        .plusMonths(1)),
                        () ->
                                assertThat(
                                                subscription
                                                        .getNextBillingAt()
                                                        .truncatedTo(ChronoUnit.MINUTES))
                                        .isEqualTo(
                                                LocalDateTime.now()
                                                        .truncatedTo(ChronoUnit.MINUTES)
                                                        .plusMonths(1)
                                                        .plusDays(1)));
            }

            @Test
            void 결제ID가_null이면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, null, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_PLAN.getMessage());
            }

            @Test
            void 존재하지_않는_결제ID면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, 999L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage());
            }

            @Test
            void 결제상태가_PAID가_아니면_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, 2L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(PaymentErrorCode.NOT_PAID.getMessage());
            }

            @Test
            void 결제한_회원과_로그인_회원이_일치하지_않으면_예외가_발생한다() {
                // given
                given(memberUtil.getCurrentMember())
                        .willReturn(memberRepository.findById(2L).get());

                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, 1L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH.getMessage());
            }

            @Test
            void 결제가_이미_사용된_경우_예외가_발생한다() {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumPlan.PRO, 3L, false);

                // when & then
                assertThatThrownBy(() -> albumService.createAlbum(request))
                        .isInstanceOf(AlbumException.class)
                        .hasMessage(PaymentErrorCode.ALREADY_USED_PAYMENT.getMessage());
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));
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
                                    .extracting("id", "title", "coverUrl", "plan")
                                    .containsExactly(
                                            1L,
                                            "testUpdatedTitle",
                                            "testUpdatedCoverUrl",
                                            AlbumPlan.BASIC));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(999L, request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(3L, request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            // when & then
            assertThatThrownBy(() -> albumService.updateAlbum(2L, request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }
    }

    @Nested
    class 앨범_권한_부여_토글_상태_변경_요청_시 {

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.PRO, true);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4));

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
            participantRepository.saveAll(
                    List.of(
                            participant1,
                            participant2,
                            participant3,
                            participant4,
                            participant5,
                            participant6));
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
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(4L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(2L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void BASIC_플랜인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.togglePermission(3L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(
                            AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_PLAN
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album2, ParticipantRole.STANDARD);
            participantRepository.saveAll(List.of(participant1, participant2));
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
                    .containsExactly(1L, createdCode.getCode(), 1800L);
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(3L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(1L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(2L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
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
        }

        @Test
        void 정렬_조건이_ASC이면_albumId를_오름차순으로_조회한다() {
            // given
            createTestAlbums();

            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbums(null, 2, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("albumId")
                                    .containsExactly(1L, 2L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 정렬_조건이_DESC이면_albumId를_내림차순으로_조회한다() {
            // given
            createTestAlbums();

            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbums(null, 2, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("albumId")
                                    .containsExactly(2L, 1L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // given
            createTestAlbums();

            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbums(null, 2, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(2),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_반환한다() {
            // given
            createTestAlbums();

            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbums(null, 1, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(1),
                    () -> assertThat(response.isLast()).isFalse());
        }

        @Test
        void 앨범이_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<AlbumListResponse> response =
                    albumService.getParticipatingAlbums(null, 10, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }

        private void createTestAlbums() {
            Member member = memberRepository.findById(1L).get();

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.PRO, false);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2));
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant =
                    Participant.createParticipant(member, album3, ParticipantRole.HOST);
            participantRepository.save(participant);

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
            invitationCodeRepository.saveAll(List.of(invitationCode1, invitationCode2));
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
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_초대_코드가_redis에_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(2L, "NoneExistingCode"))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.INVITATION_CODE_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_초대_코드가_redis에_저장된_코드와_일치하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(1L, "expiredInvitationCode"))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.INVITATION_CODE_MISMATCH.getMessage());
        }

        @Test
        void 이미_입장한_앨범에_재입장_하려는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.joinAlbum(3L, "testInvitationCode2"))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALREADY_PARTICIPATED.getMessage());
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumPlan.BASIC, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumPlan.PRO, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4, album5));

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
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(999L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(4L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(2L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 다른_참가자가_남아있는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(3L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.OTHER_PARTICIPANTS_EXIST.getMessage());
        }

        @Test
        void 구독_중인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> albumService.deleteAlbum(5L))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.SUBSCRIPTION_ACTIVE.getMessage());
        }
    }
}
