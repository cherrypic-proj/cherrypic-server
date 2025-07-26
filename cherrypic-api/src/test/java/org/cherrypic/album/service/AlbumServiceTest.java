package org.cherrypic.album.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.entity.InvitationCode;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.album.repository.InvitationCodeRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.InvitationLinkCreateRequest;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.service.AlbumService;
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
import org.springframework.transaction.annotation.Transactional;

class AlbumServiceTest extends IntegrationTest {

    @Autowired private AlbumService albumService;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private InvitationCodeRepository invitationCodeRepository;
    @Autowired private ParticipantRepository participantRepository;

    @MockitoBean MemberUtil memberUtil;

    @Transactional
    @Nested
    class 앨범을_생성할_때 {

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
        void 유효한_요청이면_앨범과_HOST_참여자가_생성된다() {
            // given
            AlbumCreateRequest request = new AlbumCreateRequest("testTitle", "testCoverUrl");

            // when
            albumService.createAlbum(request);

            // then
            Album album = albumRepository.findById(1L).orElseThrow();
            Participant participant = album.getParticipants().get(0);

            Assertions.assertAll(
                    () -> assertThat(album.getId()).isEqualTo(1L),
                    () -> assertThat(album.getTitle()).isEqualTo("testTitle"),
                    () -> assertThat(album.getCoverUrl()).isEqualTo("testCoverUrl"),
                    () -> assertThat(participant.getId()).isEqualTo(1L),
                    () -> assertThat(participant.getMember().getId()).isEqualTo(1L),
                    () -> assertThat(participant.getRole()).isEqualTo(ParticipantRole.HOST));
        }
    }

    @Nested
    class 초대_코드를_생성할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId2", "testOauthProvider2"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));

            Album album1 = Album.createAlbum("testAlbum1", "testURL1");
            Album album2 = Album.createAlbum("testAlbum2", "testURL2");
            albumRepository.saveAll(List.of(album1, album2));

            Participant participant1 =
                    Participant.createParticipant(member1, album1, ParticipantRole.HOST);
            Participant participant2 =
                    Participant.createParticipant(member2, album2, ParticipantRole.STANDARD);
            participantRepository.saveAll(List.of(participant1, participant2));
        }

        @Test
        void 유효한_요청이면_초대_코드를_저장하며_반환되는_링크의_뒤에_포함된다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(1L).get());
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(1L);

            // when
            InvitationLinkCreateResponse response = albumService.createInvitationLink(request);

            // then
            InvitationCode savedCode = invitationCodeRepository.findById(1L).orElseThrow();

            String link = response.invitationLink();
            String codeInLink = link.substring(link.lastIndexOf("=") + 1);

            assertThat(codeInLink).isEqualTo(savedCode.getCode());
        }

        @Test
        void 유효한_초대_코드가_이미_존재하는_경우_갱신하지_않는다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(1L).get());
            InvitationCode invitationCode =
                    InvitationCode.builder().albumId(1L).code("testInvitationCode").build();
            invitationCodeRepository.save(invitationCode);
            String invitationCodeBefore = invitationCode.getCode();

            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(1L);

            // when
            albumService.createInvitationLink(request);

            // then
            Optional<InvitationCode> code = invitationCodeRepository.findById(1L);
            Assertions.assertAll(
                    () -> assertThat(code).isPresent(),
                    () -> assertThat(invitationCodeBefore).isEqualTo(code.get().getCode()));
        }

        @Test
        void 유효한_요청에_대해서_유효한_초대코드가_생성된다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(1L).get());
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(1L);

            // when
            albumService.createInvitationLink(request);

            // then
            InvitationCode createdCode = invitationCodeRepository.findById(1L).orElseThrow();
            Assertions.assertAll(
                    () -> assertThat(createdCode.getAlbumId()).isEqualTo(1L),
                    () -> assertThat(createdCode.getCode().length()).isEqualTo(8),
                    () -> assertThat(createdCode.getTtl()).isEqualTo(1800L));
        }

        @Test
        void 현재_유저가_HOST가_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(2L);

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.INVALID_INVITATION_AUTHORITY.getMessage());
        }

        @Test
        void 현재_유저가_앨범_소속이_아닌_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(1L);

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.NOT_ALBUM_PARTICIPANT.getMessage());
        }

        @Test
        void 존재하지_않는_앨범_ID_를_입력한_경우_예외가_발생한다() {
            // given
            given(memberUtil.getCurrentMember()).willReturn(memberRepository.findById(2L).get());
            InvitationLinkCreateRequest request = new InvitationLinkCreateRequest(3L);

            // when & then
            assertThatThrownBy(() -> albumService.createInvitationLink(request))
                    .isInstanceOf(AlbumException.class)
                    .hasMessage(AlbumErrorCode.ALBUM_NOT_FOUND.getMessage());
        }
    }
}
