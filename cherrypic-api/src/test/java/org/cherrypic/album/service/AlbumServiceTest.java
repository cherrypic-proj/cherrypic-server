package org.cherrypic.album.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.repository.MemberRepository;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

class AlbumServiceTest extends IntegrationTest {

    @Autowired private AlbumService albumService;
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

    @Transactional
    @Nested
    class 앨범을_생성할_때 {

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
}
