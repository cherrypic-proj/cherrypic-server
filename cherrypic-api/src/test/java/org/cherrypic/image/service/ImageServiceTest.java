package org.cherrypic.image.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.AlbumImageUploadRequest;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.request.UploadFailedImageDeleteRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlsResponse;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.EventImageRepository;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.image.service.ImageService;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.entity.EventImage;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
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

class ImageServiceTest extends IntegrationTest {

    @MockitoBean MemberUtil memberUtil;

    @Autowired private ImageService imageService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EventImageRepository eventImageRepository;

    @Nested
    class Presigned_URL을_생성할_때 {

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
        void 유효한_요청이면_회원_프로필_이미지용_Presigned_URL을_생성한다() {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(FileExtension.JPEG, "testMd5Hash");

            // when
            PresignedUrlResponse response = imageService.createMemberProfileImageUploadUrl(request);

            // then
            assertThat(response.presignedUrl())
                    .containsPattern(
                            String.format(
                                    "/%s/%s/%d/[\\w\\-]+\\.jpeg", "local", "member-profile", 1));
        }

        @Test
        void 동영상_확장자를_입력할_경우_예외가_발생한다() {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(FileExtension.MKV, "testMd5Hash");

            // when & then
            assertThatThrownBy(() -> imageService.createMemberProfileImageUploadUrl(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.NOT_IMAGE_EXTENSION.getMessage());
        }
    }

    @Nested
    class 앨범_이미지_업로드_Presigned_URL들을_생성할_때 {

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
            album1.increaseCapacity(BigDecimal.ONE);
            Album album2 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC, false);
            Album album3 = Album.createAlbum("testTitle3", "testCoverUrl3", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 유효한_요청이면_이미지를_저장하고_Presigned_URL들을_반환한다() {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            // when & then
            PresignedUrlsResponse response = imageService.createAlbumImageUploadUrls(1L, request);

            assertThat(response.presignedUrls())
                    .hasSize(2)
                    .satisfiesExactly(
                            url1 ->
                                    assertThat(url1)
                                            .containsPattern(
                                                    ".*/local/album-image/1/[\\w\\-]+\\.(jpg|jpeg)\\?.+"),
                            url2 ->
                                    assertThat(url2)
                                            .containsPattern(
                                                    ".*/local/album-image/1/[\\w\\-]+\\.(jpg|jpeg)\\?.+"));

            List<Image> images = imageRepository.findAll();
            assertThat(images)
                    .hasSize(2)
                    .allSatisfy(
                            image -> {
                                assertThat(image.getAlbum().getId()).isEqualTo(1L);
                                assertThat(image.getMemberId()).isEqualTo(1L);
                                assertThat(image.getUrl())
                                        .containsPattern(
                                                String.format(
                                                        "/%s/%s/%d/[\\w\\-]+\\.(jpg|jpeg)",
                                                        "local", "album-image", 1));
                            });
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            // when & then
            assertThatThrownBy(() -> imageService.createAlbumImageUploadUrls(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            // when & then
            assertThatThrownBy(() -> imageService.createAlbumImageUploadUrls(3L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            // when & then
            assertThatThrownBy(() -> imageService.createAlbumImageUploadUrls(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }

        @Test
        void 앨범의_남은_용량을_초과해서_요청하면_예외가_발생한다() {
            /// given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.TEN,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            // when & then
            assertThatThrownBy(() -> imageService.createAlbumImageUploadUrls(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED.getMessage());
        }

        @Test
        void 해시값에_중복이_존재하면_예외가_발생한다() {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now())));
            // when & then
            assertThatThrownBy(() -> imageService.createAlbumImageUploadUrls(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.DUPLICATE_HASHES.getMessage());
        }
    }

    @Nested
    class 앨범_이미지_목록_조회_요청시 {

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
            Album album3 = Album.createAlbum("testTitle2", "testCoverUrl2", AlbumPlan.BASIC, false);
            albumRepository.saveAll(List.of(album1, album2, album3));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.HOST);
            participantRepository.saveAll(List.of(participant1, participant2));

            Event event = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            eventRepository.save(event);

            Image image1 = Image.createImage(album1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));

            EventImage eventImage = EventImage.createEventImage(event, image1);
            eventImageRepository.save(eventImage);
        }

        @Test
        void 정렬_조건이_ASC이면_imageId를_오름차순으로_조회한다() {
            // when
            SliceResponse<AlbumImageListResponse> response =
                    imageService.getAlbumImages(1L, null, 3, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(1L, 2L, 3L);
        }

        @Test
        void 정렬_조건이_DESC면_imageId를_내림차순으로_조회한다() {
            // when
            SliceResponse<AlbumImageListResponse> response =
                    imageService.getAlbumImages(1L, null, 3, SortDirection.DESC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(3L, 2L, 1L);
        }

        @Test
        void imageId를_입력하면_다음_Image_부터_조회한다() {
            // when
            SliceResponse<AlbumImageListResponse> response =
                    imageService.getAlbumImages(1L, 1L, 2, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(2L, 3L);
        }

        @Test
        void 앨범에_이미지가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<AlbumImageListResponse> response =
                    imageService.getAlbumImages(2L, null, 3, SortDirection.ASC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // when
            SliceResponse<AlbumImageListResponse> response =
                    imageService.getAlbumImages(1L, null, 3, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(3),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getAlbumImages(999L, null, 2, SortDirection.ASC))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getAlbumImages(3L, null, 2, SortDirection.ASC))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }
    }

    @Nested
    class 이벤트_이미지_목록_조회_요청시 {

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

            Participant participant =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            participantRepository.save(participant);

            Event event1 = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album1, "testTitle2", "testCoverUrl2");
            Event event3 = Event.createEvent(album2, "testTitle3", "testCoverUrl3");
            eventRepository.saveAll(List.of(event1, event2, event3));

            Image image1 = Image.createImage(album1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2));

            EventImage eventImage1 = EventImage.createEventImage(event1, image1);
            EventImage eventImage2 = EventImage.createEventImage(event1, image2);
            eventImageRepository.saveAll(List.of(eventImage1, eventImage2));
        }

        @Test
        void 정렬_조건이_ASC이면_eventImageId를_오름차순으로_조회한다() {
            // when
            SliceResponse<EventImageListResponse> response =
                    imageService.getEventImages(1L, null, 2, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("eventImageId").containsExactly(1L, 2L);
        }

        @Test
        void 정렬_조건이_DESC면_eventImageId를_내림차순으로_조회한다() {
            // when
            SliceResponse<EventImageListResponse> response =
                    imageService.getEventImages(1L, null, 2, SortDirection.DESC);

            // then
            assertThat(response.content()).extracting("eventImageId").containsExactly(2L, 1L);
        }

        @Test
        void imageId를_입력하면_다음_eventImage_부터_조회한다() {
            // when
            SliceResponse<EventImageListResponse> response =
                    imageService.getEventImages(1L, 1L, 1, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("eventImageId").containsExactly(2L);
        }

        @Test
        void 이벤트에_이미지가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<EventImageListResponse> response =
                    imageService.getEventImages(2L, null, 2, SortDirection.ASC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            // when
            SliceResponse<EventImageListResponse> response =
                    imageService.getEventImages(1L, null, 2, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(2),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getEventImages(3L, null, 2, SortDirection.ASC))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 이벤트가_존재하지_않을_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getEventImages(999L, null, 2, SortDirection.ASC))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    class 업로드_실패한_이미지_삭제할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname1",
                            "testProfileImageUrl1");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumPlan.BASIC, false);
            albumRepository.save(album);

            Image image1 = Image.createImage(album, 1L, "testUrl1", LocalDateTime.now());
            Image image2 = Image.createImage(album, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album, 2L, "testUrl3", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));
        }

        @Test
        void 유효한_요청이면_업로드_실패한_이미지를_삭제한다() {
            // given
            UploadFailedImageDeleteRequest request =
                    new UploadFailedImageDeleteRequest(List.of("testUrl1", "testUrl2"));

            // when
            imageService.deleteUploadFailedImages(request);

            // then
            assertThat(imageRepository.findAllById(List.of(1L, 2L))).isEmpty();
        }

        @Test
        void 내가_업로드하지_않은_이미지를_삭제할_경우_예외가_발생한다() {
            // given
            UploadFailedImageDeleteRequest request =
                    new UploadFailedImageDeleteRequest(List.of("testUrl3"));

            // when & then
            assertThatThrownBy(() -> imageService.deleteUploadFailedImages(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ImageErrorCode.PRESIGNED_IMAGES_NOT_MINE.getMessage());
        }
    }
}
