package org.cherrypic.tempalbum.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.repository.TempAlbumImageRepository;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.tempalbum.TempAlbum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TempAlbumServiceTest extends IntegrationTest {

    @MockitoBean private MemberUtil memberUtil;
    @Autowired private TransactionUtil transactionUtil;

    @Autowired private TempAlbumService tempAlbumService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private TempAlbumRepository tempAlbumRepository;
    @Autowired private TempAlbumImageRepository tempAlbumImageRepository;

    @Nested
    class 임시_앨범을_생성할_때 {

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

            Image image1 =
                    Image.createImage(album1, member.getId(), "testImageUrl1", LocalDateTime.now());
            Image image2 =
                    Image.createImage(album1, member.getId(), "testImageUrl2", LocalDateTime.now());
            Image image3 =
                    Image.createImage(album2, member.getId(), "testImageUrl3", LocalDateTime.now());
            Image image4 =
                    Image.createImage(album3, member.getId(), "testImageUrl4", LocalDateTime.now());
            imageRepository.saveAll(List.of(image1, image2, image3, image4));

            Participant participant1 =
                    Participant.createParticipant(member, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member, album2, ParticipantRole.LIMITED);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 유효한_요청이면_임시_앨범을_생성하고_임시_앨범에_이미지를_저장한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            // when
            tempAlbumService.createTempAlbum(1L, request);

            // then
            TempAlbum tempAlbum =
                    transactionUtil.getResult(
                            () -> {
                                TempAlbum loadedTempAlbum = tempAlbumRepository.findById(1L).get();
                                loadedTempAlbum.getTempAlbumImages().get(0);
                                return loadedTempAlbum;
                            });

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(tempAlbum)
                                    .extracting("member.id", "album.id", "expiredAt")
                                    .containsExactly(1L, 1L, LocalDate.now().plusDays(3)),
                    () ->
                            assertThat(tempAlbum.getTempAlbumImages())
                                    .extracting("id", "tempAlbum.id", "image.id")
                                    .containsExactly(tuple(1L, 1L, 1L), tuple(2L, 1L, 2L)));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 2L));

            // when & then
            assertThatThrownBy(() -> tempAlbumService.createTempAlbum(999L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_사용자가_임시_앨범을_생성하면_예외가_발생한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(4L));

            // when & then
            assertThatThrownBy(() -> tempAlbumService.createTempAlbum(3L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void LIMITED_권한의_사용자가_임시_앨범을_생성하면_예외가_발생한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(3L));

            // when & then
            assertThatThrownBy(() -> tempAlbumService.createTempAlbum(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.LIMITED_AUTHORITY.getMessage());
        }

        @Test
        void 앨범에_속하지_않은_이미지를_추가하면_예외가_발생한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest(List.of(1L, 4L));

            // when & then
            assertThatThrownBy(() -> tempAlbumService.createTempAlbum(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(AlbumErrorCode.IMAGES_NOT_IN_ALBUM.getMessage());
        }
    }
}
