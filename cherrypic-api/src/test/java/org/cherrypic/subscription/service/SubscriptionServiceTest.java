package org.cherrypic.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SubscriptionServiceTest extends IntegrationTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ParticipantRepository participantRepository;

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.PRO, false);
            Album album3 = Album.createAlbum("testAlbum3", "testURL3", AlbumPlan.PRO, false);
            Album album4 = Album.createAlbum("testAlbum4", "testURL4", AlbumPlan.PREMIUM, false);
            Album album5 = Album.createAlbum("testAlbum5", "testURL5", AlbumPlan.PREMIUM, false);
            Album album6 = Album.createAlbum("testAlbum6", "testURL6", AlbumPlan.PREMIUM, false);
            albumRepository.saveAll(List.of(album1, album2, album3, album4, album5, album6));

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
            participantRepository.saveAll(
                    List.of(participant1, participant2, participant3, participant4, participant5));

            Subscription subscription1 =
                    Subscription.createSubscription(member, album2, LocalDateTime.now());
            Subscription subscription2 =
                    Subscription.createSubscription(
                            member, album3, LocalDateTime.of(2025, 1, 1, 0, 0));
            subscriptionRepository.saveAll(List.of(subscription1, subscription2));
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
        void BASIC_플랜인_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            SubscriptionErrorCode.SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_PLAN
                                    .getMessage());
        }

        @Test
        void 구독이_존재하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(5L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND.getMessage());
        }

        @Test
        void 이미_해지된_구독이면_예외가_발생한다() {
            // given
            subscriptionService.cancelSubscription(2L);

            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_CANCELED.getMessage());
        }

        @Test
        void 종료된_구독이면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> subscriptionService.cancelSubscription(3L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_ENDED.getMessage());
        }
    }
}
