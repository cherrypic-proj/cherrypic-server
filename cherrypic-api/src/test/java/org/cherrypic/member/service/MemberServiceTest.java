package org.cherrypic.member.service;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import org.cherrypic.IntegrationTest;
import org.cherrypic.RedisCleaner;
import org.cherrypic.domain.image.event.ImageDeleteEvent;
import org.cherrypic.domain.member.dto.request.FcmTokenSaveRequest;
import org.cherrypic.domain.member.dto.request.MemberProfileUpdateRequest;
import org.cherrypic.domain.member.dto.response.MemberInfoResponse;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.member.service.MemberService;
import org.cherrypic.global.util.TransactionUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@RecordApplicationEvents
class MemberServiceTest extends IntegrationTest {

    @Autowired private TransactionUtil transactionUtil;
    @Autowired private RedisCleaner redisCleaner;
    @Autowired private StringRedisTemplate redisTemplate;

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

    @Autowired private ApplicationEvents applicationEvents;
    @Autowired private EntityManager em;

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
                            "status",
                            "localImageDeletion")
                    .containsExactly(
                            1L,
                            "testOauthProvider",
                            "testNickname",
                            "testProfileImageUrl",
                            MemberRole.USER,
                            MemberStatus.NORMAL,
                            false);
        }
    }

    @Nested
    class 회원_프로필을_수정할_때 {

        @Test
        void 유효한_요청이면_회원_닉네임을_변경한다() {
            // given
            MemberProfileUpdateRequest request =
                    new MemberProfileUpdateRequest("updateNickname", "updateProfileImageUrl");

            // when
            memberService.updateProfile(request);

            // then
            Member member = memberRepository.findById(1L).orElseThrow();
            assertThat(member)
                    .extracting("nickname", "profileImageUrl")
                    .containsExactly("updateNickname", "updateProfileImageUrl");
        }

        @Test
        void 프로필_이미지_URL이_교체되는_경우_S3에서_이미지를_삭제하는_이벤트를_발행한다() {
            // given
            MemberProfileUpdateRequest request =
                    new MemberProfileUpdateRequest("updateNickname", "updateProfileImageUrl");

            // when
            memberService.updateProfile(request);

            // then
            var events = applicationEvents.stream(ImageDeleteEvent.class).toList();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().imageUrl()).isEqualTo("testProfileImageUrl");
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

    @Nested
    class 로컬_이미지_삭제_허용_여부를_변경할_때 {

        @Test
        void 유효한_요청이면_로컬_이미지_삭제_허용_여부를_변경한다() {
            // when
            memberService.toggleLocalImageDeletion();

            // then
            Member member = memberRepository.findById(1L).orElseThrow();
            assertThat(member.getLocalImageDeletion()).isTrue();
        }
    }
}
