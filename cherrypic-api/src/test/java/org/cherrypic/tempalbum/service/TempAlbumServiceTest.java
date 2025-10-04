package org.cherrypic.tempalbum.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.cherrypic.member.entity.QMember.member;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumUpdateRequest;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumListResponse;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.cherrypic.tempalbum.enums.TempAlbumType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class TempAlbumServiceTest extends IntegrationTest {

    @Autowired private TempAlbumService tempAlbumService;

    @Autowired TempAlbumRepository tempAlbumRepository;
    @Autowired MemberRepository memberRepository;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 임시_앨범을_성성할_때 {

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
        void 유효한_요청이면_임시_앨범을_생성한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest("testTitle");

            // when
            tempAlbumService.createTempAlbum(request);

            // then
            TempAlbum tempAlbum = tempAlbumRepository.findById(1L).orElseThrow();

            assertThat(tempAlbum)
                    .extracting("member.id", "title", "capacityMb", "type", "expiredAt", "webUrl")
                    .containsExactly(
                            1L,
                            "testTitle",
                            new BigDecimal("0.00"),
                            TempAlbumType.DEFAULT,
                            LocalDate.now().plusDays(3),
                            null);
        }

        @Test
        void 임시_앨범_생성_제한을_넘기면_예외가_발생한다() {
            // given
            createTempAlbumNTimes(5);

            // when & then
            assertThatThrownBy(
                            () ->
                                    tempAlbumService.createTempAlbum(
                                            new TempAlbumCreateRequest("overLimit")))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(TempAlbumErrorCode.CREATE_OVER_LIMIT.getMessage());
        }

        private void createTempAlbumNTimes(int n) {
            for (int i = 1; i <= n; i++) {
                tempAlbumService.createTempAlbum(new TempAlbumCreateRequest("testTitle" + i));
            }
        }
    }

    @Nested
    class 임시_앨범_목록을_조회_할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            TempAlbum tempAlbum1 = TempAlbum.createTempAlbum(member, "testTitle1");
            TempAlbum tempAlbum2 = TempAlbum.createTempAlbum(member, "testTitle2");
            TempAlbum tempAlbum3 = TempAlbum.createTempAlbum(member, "testTitle3");
            tempAlbumRepository.saveAll(List.of(tempAlbum1, tempAlbum2, tempAlbum3));
        }

        @Test
        void 유효한_요청이면_임시_앨범_목록을_생성일_내림차순으로_조회한다() {
            // when
            TempAlbumListResponse response = tempAlbumService.getTempAlbums();

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().get(0).tempAlbumId()).isEqualTo(3L),
                    () -> assertThat(response.content().get(0).title()).isEqualTo("testTitle3"),
                    () -> assertThat(response.content().get(1).tempAlbumId()).isEqualTo(2L),
                    () -> assertThat(response.content().get(1).title()).isEqualTo("testTitle2"),
                    () -> assertThat(response.content().get(2).tempAlbumId()).isEqualTo(1L),
                    () -> assertThat(response.content().get(2).title()).isEqualTo("testTitle1"));
        }
    }

    @Nested
    class 임시_앨범을_수정할_때 {

        @BeforeEach
        void setUp() {
            Member member1 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname1",
                            "testProfileImageUrl1");
            Member member2 =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname2",
                            "testProfileImageUrl2");
            memberRepository.saveAll(List.of(member1, member2));
            given(memberUtil.getCurrentMember()).willReturn(member1);

            TempAlbum tempAlbum1 = TempAlbum.createTempAlbum(member1, "testTitle1");
            TempAlbum tempAlbum2 = TempAlbum.createTempAlbum(member2, "testTitle2");
            tempAlbumRepository.saveAll(List.of(tempAlbum1, tempAlbum2));
        }

        @Test
        void 유효한_요청이면_앨범_정보를_수정한다() {
            // given
            TempAlbumUpdateRequest request =
                    new TempAlbumUpdateRequest(1L, "changedTitle", "testUrl");

            // when
            tempAlbumService.updateTempAlbum(request);

            // then
            TempAlbum tempAlbum = tempAlbumRepository.findById(1L).orElseThrow();
            assertThat(tempAlbum)
                    .extracting("title", "webUrl")
                    .containsExactly("changedTitle", "testUrl");
        }

        @Test
        void 임시_앨범이_존재하지_않는_경우_예외가_발생한다() {
            // given
            TempAlbumUpdateRequest request =
                    new TempAlbumUpdateRequest(999L, "changedTitle", "testUrl");

            // when & then
            assertThatThrownBy(() -> tempAlbumService.updateTempAlbum(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND.getMessage());
        }

        @Test
        void 임시_앨범_생성자가_아닌_경우_예외가_발생한다() {
            // given
            TempAlbumUpdateRequest request =
                    new TempAlbumUpdateRequest(2L, "changedTitle", "testUrl");

            // when & then
            assertThatThrownBy(() -> tempAlbumService.updateTempAlbum(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER.getMessage());
        }
    }
}
