package org.cherrypic.event.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventUpdateRequest;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.repository.EventRepository;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.repository.MemberRepository;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.participant.repository.ParticipantRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class EventServiceTest extends IntegrationTest {

    @Autowired private EventService eventService;

    @Autowired private EventRepository eventRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ParticipantRepository participantRepository;

    @MockitoBean MemberUtil memberUtil;

    @Nested
    class 이벤트를_생성할_때 {

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 유효한_요청이면_이벤트가_생성된다() {
            // given
            EventCreateRequest request = new EventCreateRequest(1L, "testEvent", "testCoverUrl");

            // when
            eventService.createEvent(request);

            // then
            Event event = eventRepository.findById(1L).orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(event.getId()).isEqualTo(1L),
                    () -> assertThat(event.getAlbum().getId()).isEqualTo(1L),
                    () -> assertThat(event.getTitle()).isEqualTo("testEvent"),
                    () -> assertThat(event.getCoverUrl()).isEqualTo("testCoverUrl"));
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트를_생성하면_예외가_발생한다() {
            // given
            EventCreateRequest request = new EventCreateRequest(2L, "testEvent", "tesCoverUrl");

            // when & then
            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트를_생성하면_예외가_발생한다() {
            // given
            Member limitedMember = memberRepository.findById(2L).orElseThrow();
            given(memberUtil.getCurrentMember()).willReturn(limitedMember);

            EventCreateRequest request = new EventCreateRequest(1L, "testEvent", "tesCoverUrl");

            // when & then
            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }
    }

    @Nested
    class 이벤트를_수정할_때 {

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
            memberRepository.saveAll(List.of(member1, member2, member3));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.LIMITED);
            Participant participant3 =
                    Participant.createParticipant(member3, album2, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2, participant3));

            Event event1 = Event.createEvent(album1, "testEvent1", "testEventCoverUrl1");
            Event event2 = Event.createEvent(album2, "testEvent1", "testEventCoverUrl1");
            eventRepository.saveAll(List.of(event1, event2));
        }

        @Test
        void 유효한_요청이면_이벤트가_수정된다() {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when
            eventService.updateEvent(1L, request);

            // then
            Event event = eventRepository.findById(1L).orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(event.getId()).isEqualTo(1L),
                    () -> assertThat(event.getAlbum().getId()).isEqualTo(1L),
                    () -> assertThat(event.getTitle()).isEqualTo("changedTestEventTitle"),
                    () -> assertThat(event.getCoverUrl()).isEqualTo("changedTestEventCoverUrl"));
        }

        @Test
        void 존재하지_않는_이벤트를_수정_하면_예외가_발생한다() {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when % then
            assertThatThrownBy(() -> eventService.updateEvent(999L, request))
                    .isInstanceOf(EventException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트를_수정하면_예외가_발생한다() {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when & then
            assertThatThrownBy(() -> eventService.updateEvent(2L, request))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트를_수정하면_예외가_발생한다() {
            // given
            Member limitedMember = memberRepository.findById(2L).orElseThrow();
            given(memberUtil.getCurrentMember()).willReturn(limitedMember);

            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when & then
            assertThatThrownBy(() -> eventService.updateEvent(1L, request))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }
    }
}
