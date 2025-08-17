package org.cherrypic.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.notification.repository.NotificationRepository;
import org.cherrypic.domain.participant.dto.response.ParticipantListResponse;
import org.cherrypic.domain.participant.exception.ParticipantErrorCode;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.participant.service.ParticipantService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.notification.entity.Notification;
import org.cherrypic.notification.enums.NotificationType;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ParticipantServiceTest extends IntegrationTest {

    @Autowired private TransactionUtil transactionUtil;

    @Autowired private ParticipantService participantService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private NotificationRepository notificationRepository;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 앨범을_나갈_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId1", "testOauthProvider1"),
                            "testNickname1",
                            "testProfileImageUrl1");
            memberRepository.save(member1);
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId2", "testOauthProvider2"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.save(member2);
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.STANDARD);
            Participant participant2 =
                    Participant.createParticipant(member1, album3, ParticipantRole.HOST);
            Participant participant3 =
                    Participant.createParticipant(member2, album1, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            Notification notification1 =
                    Notification.createNotification(
                            member2,
                            member1,
                            album1,
                            "testTitle1",
                            "testContent1",
                            NotificationType.ALBUM);
            Notification notification2 =
                    Notification.createNotification(
                            member2,
                            member1,
                            album1,
                            "testTitle2",
                            "testContent2",
                            NotificationType.ALBUM);
            Notification notification3 =
                    Notification.createNotification(
                            member1,
                            member2,
                            album1,
                            "testTitle3",
                            "testContent3",
                            NotificationType.ALBUM);
            notificationRepository.saveAll(List.of(notification1, notification2, notification3));
        }

        @Test
        void 유효한_요청이면_참가자와_그에게_전달된_앨범_알림이_삭제된다() {
            // when
            participantService.leaveAlbum(1L);

            // then
            Album album =
                    transactionUtil.getResult(
                            () -> {
                                Album loadedAlbum = albumRepository.findById(1L).get();
                                loadedAlbum.getParticipants().size();
                                loadedAlbum.getNotifications().size();
                                return loadedAlbum;
                            });
            List<Participant> participants = album.getParticipants();
            List<Notification> notifications = album.getNotifications();

            Assertions.assertAll(
                    () ->
                            assertThat(participants)
                                    .extracting(Participant::getId)
                                    .doesNotContain(1L),
                    () ->
                            assertThat(notifications)
                                    .extracting(Notification::getId)
                                    .doesNotContain(1L));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.leaveAlbum(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.leaveAlbum(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 앨범_방장인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.leaveAlbum(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.HOST_LEAVE_NOT_ALLOWED.getMessage());
        }
    }

    @Nested
    class 참가자를_강퇴할_때 {

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.STANDARD);
            Participant participant3 =
                    Participant.createParticipant(member2, album2, ParticipantRole.HOST);
            Participant participant4 =
                    Participant.createParticipant(member1, album2, ParticipantRole.STANDARD);
            participantRepository.saveAll(
                    List.of(participant1, participant2, participant3, participant4));

            Notification notification1 =
                    Notification.createNotification(
                            member1,
                            member2,
                            album1,
                            "testTitle1",
                            "testContent1",
                            NotificationType.ALBUM);
            Notification notification2 =
                    Notification.createNotification(
                            member2,
                            member1,
                            album2,
                            "testTitle2",
                            "testContent2",
                            NotificationType.ALBUM);
            notificationRepository.saveAll(List.of(notification1, notification2));
        }

        @Test
        void 유효한_요청이면_참가자가_강퇴되고_그에게_전달된_앨범_알림이_삭제된다() {
            // when
            participantService.kickParticipant(1L, 2L);

            // then
            Album album =
                    transactionUtil.getResult(
                            () -> {
                                Album loadedAlbum = albumRepository.findById(1L).get();
                                loadedAlbum.getParticipants().size();
                                loadedAlbum.getNotifications().size();
                                return loadedAlbum;
                            });
            List<Participant> participants = album.getParticipants();
            List<Notification> notifications = album.getNotifications();

            Assertions.assertAll(
                    () ->
                            assertThat(participants)
                                    .extracting(Participant::getId)
                                    .doesNotContain(2L),
                    () ->
                            assertThat(notifications)
                                    .extracting(Notification::getId)
                                    .doesNotContain(1L));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(999L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 강퇴_요청자가_앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(3L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 강퇴_요청자가_앨범_방장이_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(2L, 3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_HOST.getMessage());
        }

        @Test
        void 앨범_방장이_자기_자신을_강퇴하려는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.HOST_SELF_KICK_NOT_ALLOWED.getMessage());
        }

        @Test
        void 강퇴_대상_참가자가_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ParticipantErrorCode.PARTICIPANT_NOT_FOUND.getMessage());
        }

        @Test
        void 강퇴_대상이_앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.kickParticipant(1L, 3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.PARTICIPANT_NOT_IN_ALBUM.getMessage());
        }
    }

    @Nested
    class 참가자_목록을_조회할_때 {

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.STANDARD);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // when
            SliceResponse<ParticipantListResponse> response =
                    participantService.getParticipants(1L, null, 2);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(2),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_반환한다() {
            // when
            SliceResponse<ParticipantListResponse> response =
                    participantService.getParticipants(1L, null, 1);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(1),
                    () -> assertThat(response.isLast()).isFalse());
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.getParticipants(999L, null, 2))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> participantService.getParticipants(2L, null, 2))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }
    }
}
