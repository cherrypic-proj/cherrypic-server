package org.cherrypic.image.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.repository.AlbumRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class ImageServiceTest extends IntegrationTest {

    @Autowired MemberUtil memberUtil;

    @Autowired private ImageService imageService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                        "testNickname",
                        "testProfileImageUrl");
        memberRepository.save(member);

        UserDetails userDetails =
                User.withUsername(member.getId().toString())
                        .password("")
                        .authorities(member.getRole().name())
                        .build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Nested
    class Presigned_URL을_생성할_때 {

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
    class 이미지_목록_조회_요청시 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumPlan.BASIC, false);
            albumRepository.save(album);

            Participant participant =
                    Participant.createParticipant(member, album, ParticipantRole.HOST);
            participantRepository.save(participant);

            Event event1 = Event.createEvent(album, "testTitle1", "testCoverUrl1");
            Event event2 = Event.createEvent(album, "testTitle2", "testCoverUrl2");
            Event event3 = Event.createEvent(album, "testTitle3", "testCoverUrl3");
            eventRepository.saveAll(List.of(event1, event2, event3));

            Image image1 = Image.createImage(album, event1, 1L, "testUrl", LocalDateTime.now());
            Image image2 = Image.createImage(album, event2, 1L, "testUrl2", LocalDateTime.now());
            Image image3 = Image.createImage(album, null, 1L, "testUrl2", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3));
        }

        @Test
        void eventId를_입력하지_않으면_앨범의_모든_이미지를_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, null, null, 3, SortDirection.ASC);

            // then
            Assertions.assertThat(response.content())
                    .extracting("eventImageId")
                    .containsExactly(1L, 2L, 3L);
        }

        @Test
        void eventId를_입력하면_특정_이벤트의_모든_이미지를_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, 1L, null, 3, SortDirection.ASC);

            // then
            Assertions.assertThat(response.content())
                    .extracting("eventImageId")
                    .containsExactly(1L);
        }

        @Test
        void 정렬_조건이_ASC이면_ImageId를_오름차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    imageService.getImages(1L, null, 2, SortDirection.ASC);

            // then
            Assertions.assertThat(response.content())
                    .extracting("eventImageId")
                    .containsExactly(1L, 2L);
        }

        @Test
        void 정렬_조건이_DESC면_eventImageId를_내림차순으로_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    eventService.getEventImages(1L, null, 2, SortDirection.DESC);

            // then
            Assertions.assertThat(response.content())
                    .extracting("eventImageId")
                    .containsExactly(2L, 1L);
        }

        @Test
        void eventImageId를_입력하면_다음_eventImage_부터_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    eventService.getEventImages(1L, 2L, 1, SortDirection.DESC);

            // then
            Assertions.assertThat(response.content())
                    .extracting("eventImageId")
                    .containsExactly(1L);
        }

        @Test
        void 이벤트에_이미지가_없는_경우_빈_리스트를_조회한다() {
            // when
            SliceResponse<ImageListResponse> response =
                    eventService.getEventImages(2L, null, 10, SortDirection.DESC);

            // when & then
            org.junit.jupiter.api.Assertions.assertAll(
                    () -> Assertions.assertThat(response.content().size()).isZero(),
                    () -> Assertions.assertThat(response.isLast()).isTrue());
        }
    }
}
