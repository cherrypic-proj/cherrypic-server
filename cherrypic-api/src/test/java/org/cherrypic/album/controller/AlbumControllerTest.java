package org.cherrypic.album.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.controller.AlbumController;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.participant.enums.ParticipantRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AlbumService albumService;

    @Nested
    class 앨범_생성_요청_시 {

        @Nested
        class BASIC_플랜인_경우 {

            @Test
            void 결제ID_없이_요청하면_앨범_생성_정보를_반환한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.BASIC, null);

                AlbumCreateResponse response =
                        new AlbumCreateResponse(1L, "testTitle", "testCoverUrl", AlbumPlan.BASIC);

                given(albumService.createAlbum(request)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                        .andExpect(jsonPath("$.data.albumId").value(1))
                        .andExpect(jsonPath("$.data.title").value("testTitle"))
                        .andExpect(jsonPath("$.data.coverUrl").value("testCoverUrl"))
                        .andExpect(jsonPath("$.data.plan").value("BASIC"));
            }

            @Test
            void 결제ID를_포함하여_요청하면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.BASIC, 1L);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new AlbumException(
                                        AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value("PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN"))
                        .andExpect(
                                jsonPath("$.data.message").value("BASIC 플랜에서는 결제 ID가 필요하지 않습니다."));
            }
        }

        @Nested
        class PRO_또는_PREMIUM_플랜인_경우 {

            @Test
            void 유효한_결제ID면_앨범_생성_정보를_반환한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, 1L);

                AlbumCreateResponse response =
                        new AlbumCreateResponse(1L, "testTitle", "testCoverUrl", AlbumPlan.PRO);

                given(albumService.createAlbum(request)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                        .andExpect(jsonPath("$.data.albumId").value(1))
                        .andExpect(jsonPath("$.data.title").value("testTitle"))
                        .andExpect(jsonPath("$.data.coverUrl").value("testCoverUrl"))
                        .andExpect(jsonPath("$.data.plan").value("PRO"));
            }

            @Test
            void 결제ID가_null이면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, null);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new AlbumException(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_PLAN));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("PAYMENT_REQUIRED_FOR_PAID_PLAN"))
                        .andExpect(jsonPath("$.data.message").value("유료 플랜은 결제 ID가 필요합니다."));
            }

            @Test
            void 존재하지_않는_결제ID면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, 999L);

                given(albumService.createAlbum(request))
                        .willThrow(new AlbumException(PaymentErrorCode.PAYMENT_NOT_FOUND));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                        .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                        .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
            }

            @Test
            void 결제상태가_PAID가_아니면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, 1L);

                given(albumService.createAlbum(request))
                        .willThrow(new AlbumException(PaymentErrorCode.NOT_PAID));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("NOT_PAID"))
                        .andExpect(jsonPath("$.data.message").value("결제가 완료되지 않아 검증에 실패했습니다."));
            }

            @Test
            void 결제한_회원과_로그인_회원이_일치하지_않으면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, 1L);

                given(albumService.createAlbum(request))
                        .willThrow(new AlbumException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                        .andExpect(jsonPath("$.data.code").value("PAYMENT_MEMBER_MISMATCH"))
                        .andExpect(jsonPath("$.data.message").value("결제한 사용자와 일치하지 않습니다."));
            }

            @Test
            void 결제가_이미_사용된_경우_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.PRO, 1L);

                given(albumService.createAlbum(request))
                        .willThrow(new AlbumException(PaymentErrorCode.ALREADY_USED_PAYMENT));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("ALREADY_USED_PAYMENT"))
                        .andExpect(jsonPath("$.data.message").value("이미 다른 앨범에 사용된 결제입니다."));
            }
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 앨범_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest(title, "testCoverUrl", AlbumPlan.BASIC, null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이름은 비워둘 수 없습니다."));
        }

        @Test
        void 앨범_이름이_20자를_초과하면_예외가_발생한다() throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest("t".repeat(21), "testCoverUrl", AlbumPlan.BASIC, null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이름은 최대 20자까지 가능합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" ", "PROO", "PREMIUMM"})
        void 앨범_플랜이_null_또는_지원하지_않는_형식이면_예외가_발생한다(String plan) throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest("testTitle", "testCoverUrl", AlbumPlan.from(plan), 1L);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("앨범 플랜은 비워둘 수 없으며, BASIC, PRO, PREMIUM만 지원됩니다."));
        }
    }

    @Nested
    class 앨범_수정_요청_시 {

        @Test
        void 유효한_요청이면_앨범_수정_정보를_반환한다() throws Exception {
            // given
            AlbumUpdateRequest request =
                    new AlbumUpdateRequest("testUpdatedTitle", "testUpdatedCoverUrl");

            AlbumUpdateResponse response =
                    new AlbumUpdateResponse(
                            1L, "testUpdatedTitle", "testUpdatedCoverUrl", AlbumPlan.BASIC);

            given(albumService.updateAlbum(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.albumId").value(1))
                    .andExpect(jsonPath("$.data.title").value("testUpdatedTitle"))
                    .andExpect(jsonPath("$.data.coverUrl").value("testUpdatedCoverUrl"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            given(albumService.updateAlbum(1L, request))
                    .willThrow(new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            given(albumService.updateAlbum(1L, request))
                    .willThrow(new AlbumException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            given(albumService.updateAlbum(1L, request))
                    .willThrow(new AlbumException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 앨범_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest(title, "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이름은 비워둘 수 없습니다."));
        }

        @Test
        void 앨범_이름이_20자를_초과하면_예외가_발생한다() throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("t".repeat(21), "testCoverUrl");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이름은 최대 20자까지 가능합니다."));
        }
    }

    @Nested
    class 앨범_초대_코드_생성_요청_시 {

        @Test
        void 유효한_요청이면_초대_코드_정보를_반환한다() throws Exception {
            // given
            Long albumId = 1L;

            InvitationLinkCreateResponse response =
                    new InvitationLinkCreateResponse(
                            "https://dev-api.cherrypic.today/albums/join?albumId=1&code=3FA7A9");

            given(albumService.createInvitationLink(albumId)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/{albumId}/invitation-link", albumId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(albumId)));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(
                            jsonPath("$.data.invitationLink")
                                    .value(
                                            "https://dev-api.cherrypic.today/albums/join?albumId=1&code=3FA7A9"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.createInvitationLink(999L))
                    .willThrow(new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/999/invitation-link")
                                    .contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.createInvitationLink(1L))
                    .willThrow(new AlbumException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/invitation-link")
                                    .contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.createInvitationLink(1L))
                    .willThrow(new AlbumException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/invitation-link")
                                    .contentType(MediaType.APPLICATION_JSON));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }
    }

    @Nested
    class 앨범_목록_조회_요청_시 {

        @Test
        void 정렬_조건이_ASC이면_albumId를_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(1L, "first", "coverUrl1", AlbumPlan.BASIC),
                            new AlbumListResponse(2L, "second", "coverUrl2", AlbumPlan.PRO));

            given(albumService.getParticipatingAlbums(null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "2").param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].albumId").value(1))
                    .andExpect(jsonPath("$.data.content[1].albumId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_DESC이면_albumId를_내림차순으로_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(2L, "second", "coverUrl2", AlbumPlan.PRO),
                            new AlbumListResponse(1L, "first", "coverUrl1", AlbumPlan.BASIC));

            given(albumService.getParticipatingAlbums(null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "2").param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].albumId").value(2))
                    .andExpect(jsonPath("$.data.content[1].albumId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(new AlbumListResponse(1L, "first", "coverUrl1", AlbumPlan.BASIC));

            given(albumService.getParticipatingAlbums(null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "1").param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].albumId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(2L, "second", "coverUrl2", AlbumPlan.PRO),
                            new AlbumListResponse(1L, "first", "coverUrl1", AlbumPlan.BASIC));

            given(albumService.getParticipatingAlbums(null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "1").param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].albumId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 앨범이_없는_경우_빈_리스트를_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums = List.of();

            given(albumService.getParticipatingAlbums(null, 10, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "10").param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }
    }

    @Nested
    class 앨범_입장_요청_시 {

        @Test
        void 유효한_요청이면_참가자_정보를_반환한다() throws Exception {
            // given
            AlbumJoinResponse response =
                    new AlbumJoinResponse(1L, 1L, 1L, ParticipantRole.STANDARD);

            given(albumService.joinAlbum(1L, "testInvitationCode")).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "testInvitationCode"));

            perform.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                    .andExpect(jsonPath("$.data.participantId").value(1))
                    .andExpect(jsonPath("$.data.albumId").value(1))
                    .andExpect(jsonPath("$.data.memberId").value(1))
                    .andExpect(jsonPath("$.data.role").value("STANDARD"));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(999L, "testInvitationCode"))
                    .willThrow(new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/999/join").param("code", "testInvitationCode"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_초대_코드가_redis에_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "NoneExistingCode"))
                    .willThrow(new AlbumException(AlbumErrorCode.INVITATION_CODE_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "NoneExistingCode"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("INVITATION_CODE_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범의 초대 코드가 만료되었습니다."));
        }

        @Test
        void 앨범_초대_코드가_앨범의_현재_코드와_일치하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "ExpiredInvitationCode"))
                    .willThrow(new AlbumException(AlbumErrorCode.INVITATION_CODE_MISMATCH));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "ExpiredInvitationCode"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("INVITATION_CODE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("초대코드가 올바르지 않습니다."));
        }
    }
}
