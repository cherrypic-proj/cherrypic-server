package org.cherrypic.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.RedisCleaner;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.notification.repository.NotificationRepository;
import org.cherrypic.domain.notification.service.FcmService;
import org.cherrypic.domain.notification.service.NotificationService;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.notification.entity.Notification;
import org.cherrypic.notification.enums.NotificationType;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotificationServiceTest extends IntegrationTest {

    @Autowired private RedisCleaner redisCleaner;
    @Autowired private StringRedisTemplate redisTemplate;

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ParticipantRepository participantRepository;

    @MockitoBean private MemberUtil memberUtil;
    @MockitoBean private FcmService fcmService;

    @Nested
    class 앨범_삭제_푸시_알림을_보낼_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId1", "testOauthProvider1"),
                            "hostNickname",
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
            memberRepository.saveAll(List.of(member1, member2, member3));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album = Album.createAlbum("testAlbum", "testURL", AlbumPlan.BASIC, false);
            albumRepository.save(album);

            Participant participant1 =
                    Participant.createParticipant(member1, album, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album, ParticipantRole.STANDARD);
            Participant participant3 =
                    Participant.createParticipant(member3, album, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            redisTemplate.opsForSet().add("fcmToken:2", "testFcmToken2");
        }

        @AfterEach
        void cleanUp() {
            redisCleaner.flushAll();
        }

        @Test
        void 방장을_제외한_모든_참가자의_알림을_저장하고_FCM_토큰이_있는_참가자에게만_알림을_발송한다() {
            // given
            Long albumId = 1L;
            Long senderId = 1L;
            List<Long> receiverIds = List.of(2L, 3L);

            // when
            notificationService.sendAlbumDeleteNotification(albumId, senderId, receiverIds);

            // then
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications)
                    .extracting(
                            n -> n.getReceiver().getId(),
                            n -> n.getAlbum().getId(),
                            n -> n.getSender().getId(),
                            Notification::getType)
                    .containsExactlyInAnyOrder(
                            tuple(2L, 1L, 1L, NotificationType.ALBUM),
                            tuple(3L, 1L, 1L, NotificationType.ALBUM));

            verify(fcmService)
                    .sendGroupMessageAsync(
                            eq(List.of("testFcmToken2")),
                            eq("앨범 삭제 안내"),
                            eq("hostNickname님이 testAlbum 앨범을 삭제하려고 합니다. 이미지를 백업한 후 앨범에서 나가주세요."));
        }
    }
}
