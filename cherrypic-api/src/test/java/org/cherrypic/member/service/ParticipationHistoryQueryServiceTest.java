package org.cherrypic.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.cherrypic.IntegrationTest;
import org.cherrypic.album.entity.AlbumParticipationHistory;
import org.cherrypic.album.enums.ParticipationAction;
import org.cherrypic.domain.album.repository.AlbumParticipationHistoryRepository;
import org.cherrypic.domain.member.dto.response.ParticipationHistoryResponse;
import org.cherrypic.domain.member.repository.MemberRepository;
import org.cherrypic.domain.member.service.ParticipationHistoryQueryService;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ParticipationHistoryQueryServiceTest extends IntegrationTest {

    @Autowired private ParticipationHistoryQueryService participationHistoryQueryService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AlbumParticipationHistoryRepository albumParticipationHistoryRepository;

    @MockitoBean private MemberUtil memberUtil;

    @Nested
    class 앨범의_참여_이력을_조회할_때 {

        @BeforeEach
        void setUp() {
            Member member =
                    Member.createMember(
                            OauthInfo.createOauthInfo("testOauthId", "testOauthProvider"),
                            "testNickname",
                            "testProfileImageUrl");
            memberRepository.save(member);
            given(memberUtil.getCurrentMember()).willReturn(member);

            AlbumParticipationHistory participationHistory1 =
                    AlbumParticipationHistory.createAlbumParticipationHistory(
                            member.getId(), "testTitle1", ParticipationAction.JOIN);
            AlbumParticipationHistory participationHistory2 =
                    AlbumParticipationHistory.createAlbumParticipationHistory(
                            member.getId(), "testTitle2", ParticipationAction.LEAVE);
            AlbumParticipationHistory participationHistory3 =
                    AlbumParticipationHistory.createAlbumParticipationHistory(
                            member.getId(), "testTitle3", ParticipationAction.KICK);

            albumParticipationHistoryRepository.saveAll(
                    List.of(participationHistory1, participationHistory2, participationHistory3));
        }

        @Test
        void 정렬_조건이_ASC이면_historyId를_오름차순으로_조회한다() {
            // when
            SliceResponse<ParticipationHistoryResponse> response =
                    participationHistoryQueryService.getParticipationHistory(
                            null, 3, SortDirection.ASC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("historyId")
                                    .containsExactly(1L, 2L, 3L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 정렬_조건이_DESC이면_historyId를_내림차순으로_조회한다() {
            // when
            SliceResponse<ParticipationHistoryResponse> response =
                    participationHistoryQueryService.getParticipationHistory(
                            null, 3, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () ->
                            assertThat(response.content())
                                    .extracting("historyId")
                                    .containsExactly(3L, 2L, 1L),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_반환한다() {
            SliceResponse<ParticipationHistoryResponse> response =
                    participationHistoryQueryService.getParticipationHistory(
                            null, 3, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(3),
                    () -> assertThat(response.isLast()).isTrue());
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_반환한다() {
            SliceResponse<ParticipationHistoryResponse> response =
                    participationHistoryQueryService.getParticipationHistory(
                            null, 1, SortDirection.DESC);

            // then
            Assertions.assertAll(
                    () -> assertThat(response.content().size()).isEqualTo(1),
                    () -> assertThat(response.isLast()).isFalse());
        }
    }
}
