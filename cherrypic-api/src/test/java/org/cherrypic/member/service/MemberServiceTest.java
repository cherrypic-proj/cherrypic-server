package org.cherrypic.member.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.cherrypic.IntegrationTest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.cherrypic.member.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class MemberServiceTest extends IntegrationTest {

    @Autowired private MemberService memberService;
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

    @Test
    void 회원_정보를_조회한다() {
        // when
        MemberInfoResponse response = memberService.getMemberInfo();

        // then
        Assertions.assertAll(
                () -> assertThat(response.memberId()).isEqualTo(1L),
                () -> assertThat(response.oauthProvider()).isEqualTo("testOauthProvider"),
                () -> assertThat(response.nickname()).isEqualTo("testNickname"),
                () -> assertThat(response.profileImageUrl()).isEqualTo("testProfileImageUrl"),
                () -> assertThat(response.role()).isEqualTo(MemberRole.USER),
                () -> assertThat(response.status()).isEqualTo(MemberStatus.NORMAL));
    }
}
