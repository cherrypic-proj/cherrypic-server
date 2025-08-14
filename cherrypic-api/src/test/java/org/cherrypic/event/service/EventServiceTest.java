package org.cherrypic.event.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import jakarta.persistence.EntityManager;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventImageAddRequest;
import org.cherrypic.domain.event.dto.request.EventImageRemoveRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.EventImageRepository;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.entity.EventImage;
import org.cherrypic.exception.CustomException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

public class EventServiceTest extends IntegrationTest {

    @Autowired private TransactionUtil transactionUtil;

    @Autowired private EventService eventService;

    @Autowired private EventRepository eventRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ImageRepository imageRepository;
    @MockitoSpyBean private EventImageRepository eventImageRepository;
    @Autowired private EntityManager em;

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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
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
                    .isInstanceOf(CustomException.class)
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
                    .isInstanceOf(CustomException.class)
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURL2", AlbumPlan.BASIC, false);
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
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트를_수정하면_예외가_발생한다() {
            // given
            EventUpdateRequest request =
                    new EventUpdateRequest("changedTestEventTitle", "changedTestEventCoverUrl");

            // when & then
            assertThatThrownBy(() -> eventService.updateEvent(2L, request))
                    .isInstanceOf(CustomException.class)
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
                    .isInstanceOf(CustomException.class)
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

            Album album1 = Album.createAlbum("testAlbum1", "testURL1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testAlbum2", "testURl2", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album1, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event1 = Event.createEvent(album1, "testEvent1", "testEventCoverUrl1");
            Event event2 = Event.createEvent(album2, "testEvent2", "testEventCoverUrl2");
            eventRepository.saveAll(List.of(event1, event2));
        }

        @Test
        void 유효한_요청일_경우_이벤트를_삭제한다() {
            // when
            eventService.deleteEvent(1L);

            // then
            assertThat(eventRepository.findById(1L).isPresent()).isFalse();
        }

        @Test
        void 존재하지_않는_이벤트를_삭제하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트를_삭제하면_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(2L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트를_수정하면_예외가_발생한다() {
            // given
            Member limitedMember = memberRepository.findById(2L).orElseThrow();
            given(memberUtil.getCurrentMember()).willReturn(limitedMember);

            // when & then
            assertThatThrownBy(() -> eventService.deleteEvent(1L))
                    .isInstanceOf(CustomException.class)
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

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC, false);
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

    @Nested
    class 이벤트에_이미지를_추가할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event1 = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album2, "testTitle2", "testCoverUrl2");
            eventRepository.saveAll(List.of(event1, event2));

            Image image1 = Image.createImage(album1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album2, 1L, "testUrl3", LocalDateTime.now());
            Image image4 = Image.createImage(album1, 1L, "testUrl4", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3, image4));

            EventImage eventImage = EventImage.createEventImage(event1, image2);
            eventImageRepository.save(eventImage);
        }

        @Test
        void 유효한_요청이면_이벤트에_이미지를_추가한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 4L));

            // when
            eventService.addImages(1L, request);

            // then
            List<EventImage> eventImages = eventImageRepository.findAllById(List.of(2L, 3L));
            assertThat(eventImages)
                    .extracting("event.id", "image.id")
                    .containsExactly(tuple(1L, 1L), tuple(1L, 4L));
        }

        @Test
        void 존재하지_않는_이벤트에_추가하면_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L));

            // when & then
            assertThatThrownBy(() -> eventService.addImages(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 존재하지_않는_이미지를_추가하면_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 999L));

            // when & then
            assertThatThrownBy(() -> eventService.addImages(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.IMAGES_NOT_FOUND.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트에_이미지를_추가하면_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(3L));

            // when & then
            assertThatThrownBy(() -> eventService.addImages(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }

        @Test
        void 이미_해당_이벤트에_속한_이미지를_똑같은_이벤트에_추가해도_중복으로_추가되지_않는다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(2L));

            // when
            eventService.addImages(1L, request);

            // then
            assertThat(eventImageRepository.findById(2L).isPresent()).isFalse();
        }

        @Test
        void 추가하고자_하는_이미지가_이미_추가된_동시성_충돌_시_재실행_된다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 4L));

            AtomicBoolean firstCall = new AtomicBoolean(true);
            doAnswer(
                            invocation -> {
                                if (firstCall.getAndSet(false)) {
                                    var sqlEx =
                                            new SQLIntegrityConstraintViolationException(
                                                    "Duplicate entry '1-4' for key 'uk_event_image_event_id_image_id'",
                                                    "23000",
                                                    1062);
                                    throw new DataIntegrityViolationException(
                                            "constraint violation", sqlEx);
                                } else {
                                    List<EventImage> args = invocation.getArgument(0);
                                    args.forEach(em::persist);
                                    em.flush();
                                    return args;
                                }
                            })
                    .when(eventImageRepository)
                    .saveAllAndFlush(anyList());

            // when
            eventService.addImages(1L, request);

            // then
            List<EventImage> eventImages = eventImageRepository.findAllById(List.of(2L, 3L));
            assertThat(eventImages)
                    .extracting("event.id", "image.id")
                    .containsExactly(tuple(1L, 1L), tuple(1L, 4L));
        }

        @Test
        void 추가하고자_하는_이미지가_삭제되는_동시성_충돌_시_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 4L));

            doAnswer(
                            invocation -> {
                                var sqlEx =
                                        new SQLIntegrityConstraintViolationException(
                                                "Cannot add or update a child row: a foreign key constraint fails "
                                                        + "(`testdb`.`event_image`, CONSTRAINT `fk_event_image_image` FOREIGN KEY (`image_id`) "
                                                        + "REFERENCES `image` (`id`))",
                                                "23000",
                                                1452);
                                throw new DataIntegrityViolationException(
                                        "constraint violation", sqlEx);
                            })
                    .when(eventImageRepository)
                    .saveAllAndFlush(anyList());

            // when & then
            assertThatThrownBy(() -> eventService.addImages(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.IMAGE_DELETED.getMessage());
        }

        @Test
        void 추가하고자_하는_이벤트가_삭제되는_동시성_충돌_시_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 4L));

            doAnswer(
                            invocation -> {
                                var sqlEx =
                                        new SQLIntegrityConstraintViolationException(
                                                "Cannot add or update a child row: a foreign key constraint fails "
                                                        + "(`testdb`.`event_image`, CONSTRAINT `fk_event_image_event` FOREIGN KEY (`event_id`) "
                                                        + "REFERENCES `event` (`id`))",
                                                "23000",
                                                1452);
                                throw new DataIntegrityViolationException(
                                        "constraint violation", sqlEx);
                            })
                    .when(eventImageRepository)
                    .saveAllAndFlush(anyList());

            // when & then
            assertThatThrownBy(() -> eventService.addImages(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_DELETED.getMessage());
        }

        @Test
        void 예상하지_못한_DB_동시성_에러_발생시_예외가_발생한다() {
            // given
            EventImageAddRequest request = new EventImageAddRequest(List.of(1L, 4L));

            doAnswer(
                            invocation -> {
                                var sqlEx =
                                        new SQLIntegrityConstraintViolationException(
                                                "random test SQL constraint message that does not contain known constraint names",
                                                "23000",
                                                9999);
                                throw new DataIntegrityViolationException(
                                        "constraint violation", sqlEx);
                            })
                    .when(eventImageRepository)
                    .saveAllAndFlush(anyList());

            // when & then
            assertThatThrownBy(() -> eventService.addImages(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.IMAGE_CONFLICT.getMessage());
        }
    }

    @Nested
    class 이벤트에_이미지를_제거할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album1 = Album.createAlbum("testTitle1", "testCoverUrl1", AlbumPlan.BASIC, false);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testTitle3", "testCoverUrl3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event1 = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album2, "testTitle2", "testCoverUrl2");
            Event event3 = Event.createEvent(album3, "testTitle3", "testCoverUrl3");
            Event event4 = Event.createEvent(album1, "testTitle4", "testCoverUrl4");
            eventRepository.saveAll(List.of(event1, event2, event3, event4));

            Image image1 = Image.createImage(album1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album2, 1L, "testUrl3", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));

            EventImage eventImage1 = EventImage.createEventImage(event1, image1);
            EventImage eventImage2 = EventImage.createEventImage(event1, image2);
            EventImage eventImage3 = EventImage.createEventImage(event4, image3);

            eventImageRepository.saveAll(List.of(eventImage1, eventImage2, eventImage3));
        }

        @Test
        void 유효한_요청이면_이벤트_이미지가_삭제된다() {
            // given
            EventImageRemoveRequest request = new EventImageRemoveRequest(List.of(1L, 2L));

            // when
            eventService.removeImages(1L, request);

            // then
            assertThat(eventImageRepository.findAllById(List.of(1L, 2L))).isEmpty();
        }

        @Test
        void 존재하지_않는_이벤트를_입력하면_예외가_발생한다() {
            // given
            EventImageRemoveRequest request = new EventImageRemoveRequest(List.of(1L, 2L));

            // when & then
            assertThatThrownBy(() -> eventService.removeImages(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_이벤트_이미지를_삭제하면_예외가_발생한다() {
            // given
            EventImageRemoveRequest request = new EventImageRemoveRequest(List.of(1L, 2L));

            // when & then
            assertThatThrownBy(() -> eventService.removeImages(3L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_이벤트_이미지를_삭제하면_예외가_발생한다() {
            // given
            EventImageRemoveRequest request = new EventImageRemoveRequest(List.of(3L));

            // when & then
            assertThatThrownBy(() -> eventService.removeImages(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }

        @Test
        void 이벤트와_EventImage의_이벤트가_일치하지_않는_경우_예외가_발생한다() {
            // given
            EventImageRemoveRequest request = new EventImageRemoveRequest(List.of(3L));

            // when & then
            assertThatThrownBy(() -> eventService.removeImages(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_IMAGE_NOT_FROM_EVENT.getMessage());
        }
    }
}
