package org.cherrypic.tempalbum.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.cherrypic.IntegrationTest;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.cherrypic.tempalbum.enums.TempAlbumType;
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
        void 유효한_요청이면_임시앨범을_생성한다() {
            // given
            TempAlbumCreateRequest request = new TempAlbumCreateRequest("testTitle");

            // when
            tempAlbumService.createTempAlbum(request);

            // then
            TempAlbum tempAlbum = tempAlbumRepository.findById(1L).orElseThrow();

            assertThat(tempAlbum)
                    .extracting("member.id", "title", "capacityGb", "type", "expiredAt", "url")
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
}
