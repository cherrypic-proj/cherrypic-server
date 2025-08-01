package org.cherrypic.event.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.repository.EventImageRepository;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.entity.EventImage;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class EventServiceTest extends IntegrationTest {

    @Autowired private TransactionUtil transactionUtil;

    @Autowired private EventService eventService;

    @Autowired private EventRepository eventRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private EventImageRepository eventImageRepository;

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
            assertThat(event)
                    .extracting("id", "album.id", "title", "coverUrl")
                    .containsExactly(1L, 1L, "testEvent", "testCoverUrl");
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
            assertThat(event)
                    .extracting("id", "album.id", "title", "coverUrl")
                    .containsExactly(1L, 1L, "changedTestEventTitle", "changedTestEventCoverUrl");
        }

        @Test
        void 존재하지_않는_이벤트를_수정_하면_예외가_발생한다() {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when & then
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

    @Nested
    class 이벤트를_삭제할_때 {

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
            Album album2 = Album.createAlbum("testAlbum2", "testURl2", AlbumPlan.BASIC);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event1 = Event.createEvent(album1, "testEvent1", "testEventCoverUrl1");
            Event event2 = Event.createEvent(album2, "testEvent2", "testEventCoverUrl2");
            eventRepository.saveAll(List.of(event1, event2));

            Image image1 = Image.createImage(album1, 1L, "testImageUrl1", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testImageUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album1, 1L, "testImageUrl3", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));

            EventImage eventImage1 = EventImage.createEventImage(event1, image1);
            EventImage eventImage2 = EventImage.createEventImage(event1, image2);
            EventImage eventImage3 = EventImage.createEventImage(event1, image3);
            eventImageRepository.saveAll(List.of(eventImage1, eventImage2, eventImage3));
        }

        @Test
        void 유효한_요청일_경우_이벤트와_이벤트_이미지를_모두_삭제한다() {
            // given
            Event event =
                    transactionUtil.getResult(
                            () -> {
                                Event loadedEvent = eventRepository.findById(1L).orElseThrow();
                                loadedEvent.getEventImages().get(0);
                                return loadedEvent;
                            });

            assertThat(event.getEventImages())
                    .hasSize(3)
                    .extracting("id", "event.id", "image.id")
                    .containsExactly(tuple(1L, 1L, 1L), tuple(2L, 1L, 2L), tuple(3L, 1L, 3L));

            // when
            eventService.deleteEvent(1L);

            // then
            Assertions.assertAll(
                    () -> assertThat(eventRepository.findById(1L).isPresent()).isFalse(),
                    () -> assertThat(eventImageRepository.countByEventId(1L)).isEqualTo(0L));
        }

        @Test
        void 존재하지_않는_이벤트를_삭제하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(999L))
                    .isInstanceOf(EventException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트를_삭제하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(2L))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트를_수정하면_예외가_발생한다() {
            // given
            Member limitedMember = memberRepository.findById(2L).orElseThrow();
            given(memberUtil.getCurrentMember()).willReturn(limitedMember);

            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(1L))
                    .isInstanceOf(EventException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }
    }

    @Nested
    class 이벤트_목록을_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumPlan.BASIC);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event1 = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album1, "testTitle2", "testCoverUrl2");
            eventRepository.saveAll(List.of(event1, event2));

            Image image1 = Image.createImage(album1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2));

            EventImage eventImage1 = EventImage.createEventImage(event1, image1);
            EventImage eventImage2 = EventImage.createEventImage(event1, image2);
            eventImageRepository.saveAll(List.of(eventImage1, eventImage2));
        }

        @Test
        void 정렬_조건이_ASC이면_eventId를_오름차순으로_조회한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(1L, null, 2, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () ->
                            org.assertj.core.api.Assertions.assertThat(response.content())
                                    .extracting("eventId", "numberOfImages")
                                    .containsExactly(tuple(1L, 2), tuple(2L, 0)),
                    () -> org.assertj.core.api.Assertions.assertThat(response.isLast()).isTrue());
        }

        @Test
        void 정렬_조건이_DESC면_eventId를_내림차순으로_조회한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(1L, null, 2, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () ->
                            org.assertj.core.api.Assertions.assertThat(response.content())
                                    .extracting("eventId", "numberOfImages")
                                    .containsExactly(tuple(2L, 0), tuple(1L, 2)),
                    () -> org.assertj.core.api.Assertions.assertThat(response.isLast()).isTrue());
        }

        @Test
        void eventId를_입력하면_다음_event_부터_조회한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(1L, 2L, 1, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () ->
                            org.assertj.core.api.Assertions.assertThat(response.content())
                                    .extracting("eventId", "numberOfImages")
                                    .containsExactly(tuple(1L, 2)),
                    () -> org.assertj.core.api.Assertions.assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(1L, null, 2, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(2),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_반환한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(1L, null, 1, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(1),
                    () -> assertThat(response.isLast()).isFalse());
        }

        @Test
        void 이벤트가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<EventListResponse> response =
                    eventService.getAlbumEvents(2L, null, 10, SortDirection.DESC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }
    }
}
