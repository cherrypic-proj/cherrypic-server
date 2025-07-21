package org.cherrypic.event.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.service.EventService;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.repository.EventRepository;
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

public class EventServiceTest extends IntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private EventRepository eventRepository;
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
    class 이벤트를_생성할_때 {

        @BeforeEach
        void setUp() {
            Album album = Album.createAlbum("Test Album", "Test URL", AlbumType.SHARED);
            albumRepository.save(album);
            Participant participant =
                    Participant.createParticipant(
                            memberRepository.findById(1L).orElseThrow(),
                            album,
                            ParticipantRole.HOST);
            album.addParticipant(participant);
        }

        @Test
        void 유효한_요청이면_이벤트가_생성된다() {

            // given
            EventCreateRequest request = new EventCreateRequest(1L, "Test Event", "Test CoverURL");

            // when
            eventService.createEvent(request);

            // then
            Event event = eventRepository.findById(1L).orElseThrow();

            Assertions.assertAll(
                    () -> assertThat(event.getId()).isEqualTo(1L),
                    () -> assertThat(event.getAlbum().getId()).isEqualTo(1L),
                    () -> assertThat(event.getTitle()).isEqualTo("Test Event"),
                    () -> assertThat(event.getCoverUrl()).isEqualTo("Test CoverURL"));
        }
    }
}
