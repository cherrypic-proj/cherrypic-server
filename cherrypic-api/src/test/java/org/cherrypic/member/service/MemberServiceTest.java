package org.cherrypic.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import org.cherrypic.IntegrationTest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MemberServiceTest extends IntegrationTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @MockitoBean private MemberUtil memberUtil;

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

    @Nested
    class 회원_정보를_조회할_때 {

        @Test
        void 유효한_요청이면_회원_정보를_조회한다() {
            // when
            MemberInfoResponse response = memberService.getMemberInfo();

            // then
            assertThat(response)
                    .extracting(
                            "memberId",
                            "oauthProvider",
                            "nickname",
                            "profileImageUrl",
                            "role",
                            "status")
                    .containsExactly(
                            1L,
                            "testOauthProvider",
                            "testNickname",
                            "testProfileImageUrl",
                            MemberRole.USER,
                            MemberStatus.NORMAL);
        }
    }
}
