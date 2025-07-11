package org.cherrypic.member.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Optional;
import org.cherrypic.IntegrationTest;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberRepositoryTest extends IntegrationTest {

    @Autowired private MemberRepository memberRepository;

    @Nested
    class OauthInfo로_회원_조회할_때 {

        @Test
        void 회원이_존재하면_조회에_성공한다() {
            // given
            OauthInfo oauthInfo = OauthInfo.createOauthInfo("testOauthId", "testOauthProvider");
            Member member = Member.createMember(oauthInfo, "testNickname", "testProfileImageUrl");

            memberRepository.save(member);

            // when
            Optional<Member> result = memberRepository.findByOauthInfo(oauthInfo);

            // then
            assertThat(result).isPresent();
        }

        @Test
        void 회원이_존재하지_않으면_조회에_실패한다() {
            // when
            Optional<Member> result =
                    memberRepository.findByOauthInfo(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"));

            // then
            assertThat(result).isEmpty();
        }
    }
}
