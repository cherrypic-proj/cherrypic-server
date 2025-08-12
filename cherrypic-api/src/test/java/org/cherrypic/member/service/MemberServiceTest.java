package org.cherrypic.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import org.cherrypic.IntegrationTest;
import org.cherrypic.RedisCleaner;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MemberServiceTest extends IntegrationTest {

    @Autowired private RedisCleaner redisCleaner;
    @Autowired private StringRedisTemplate redisTemplate;

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

    @Nested
    class FCM_토큰을_저장할_때 {

        @AfterEach
        void cleanUp() {
            redisCleaner.flushAll();
        }

        @Test
        void 유효한_요청이면_토큰이_Redis_Set에_저장된다() {
            // given
            FcmTokenSaveRequest request = new FcmTokenSaveRequest("testFcmToken");

            // when
            memberService.saveFcmToken(request);

            // then
            assertThat(redisTemplate.opsForSet().members("fcmToken:1")).contains("testFcmToken");
        }

        @Test
        void 여러_디바이스_토큰을_저장하면_모두_Redis_Set에_저장된다() {
            // given
            FcmTokenSaveRequest request1 = new FcmTokenSaveRequest("testFcmToken1");
            FcmTokenSaveRequest request2 = new FcmTokenSaveRequest("testFcmToken2");

            // when
            memberService.saveFcmToken(request1);
            memberService.saveFcmToken(request2);

            // then
            assertThat(redisTemplate.opsForSet().members("fcmToken:1"))
                    .contains("testFcmToken1", "testFcmToken2");
            assertThat(redisTemplate.opsForSet().size(("fcmToken:1"))).isEqualTo(2);
        }
    }
}
