package org.cherrypic.image.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.ImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.image.service.ImageService;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
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
                    new MemberProfileImageUploadRequest(ImageFileExtension.JPEG);

            // when
            PresignedUrlResponse response = imageService.createMemberProfileImageUploadUrl(request);

            // then
            assertThat(response.presignedUrl())
                    .containsPattern(
                            String.format(
                                    "/%s/%s/%d/[\\w\\-]+\\.jpeg", "local", "member-profile", 1));
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

            Event event1 = Event.createEvent(album1, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album1, "testTitle2", "testCoverUrl2");
            Event event3 = Event.createEvent(album1, "testTitle3", "testCoverUrl3");
            eventRepository.saveAll(List.of(event1, event2, event3));

            Image image1 = Image.createImage(album1, event1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, event2, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album1, null, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));
        }

        @Test
        void 정렬_조건이_ASC이면_imageId를_오름차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, null, null, 3, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(1L, 2L, 3L);
        }

        @Test
        void 정렬_조건이_DESC면_imageId를_내림차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, null, null, 3, SortDirection.DESC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(3L, 2L, 1L);
        }

        @Test
        void imageId를_입력하면_다음_Image_부터_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, null, 1L, 2, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(2L, 3L);
        }

        @Test
        void 앨범에_이미지가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(2L, null, 1L, 2, SortDirection.ASC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(999L, null, null, 2, SortDirection.ASC))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(3L, null, null, 2, SortDirection.ASC))
                    .isInstanceOf(AlbumException.class)
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

            Image image1 = Image.createImage(album1, event1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album1, event1, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2));
        }

        @Test
        void 정렬_조건이_ASC이면_imageId를_오름차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, 1L, null, 2, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(1L, 2L);
        }

        @Test
        void 정렬_조건이_DESC면_imageId를_내림차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, 1L, null, 2, SortDirection.DESC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(2L, 1L);
        }

        @Test
        void imageId를_입력하면_다음_Image_부터_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, 1L, 1L, 1, SortDirection.ASC);

            // then
            assertThat(response.content()).extracting("imageId").containsExactly(2L);
        }

        @Test
        void 이벤트에_이미지가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, 2L, 1L, 2, SortDirection.ASC);

            // when & then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isZero(),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(999L, null, null, 2, SortDirection.ASC))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(2L, null, null, 2, SortDirection.ASC))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 이벤트가_존재하지_않을_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(1L, 999L, null, 2, SortDirection.ASC))
                    .isInstanceOf(EventException.class)
                    .hasMessage(EventErrorCode.EVENT_NOT_FOUND.getMessage());
        }

        @Test
        void 이벤트가_앨범에_속하지_않는_경우_예외가_발생한다() {
            // when & then
            assertThatThrownBy(() -> imageService.getImages(1L, 3L, null, 2, SortDirection.ASC))
                    .isInstanceOf(EventException.class)
                    .hasMessage(EventErrorCode.EVENT_DOESNT_BELONG_TO_ALBUM.getMessage());
        }
    }
}
