package org.cherrypic.album.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.controller.AlbumController;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
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
            void 결제가_이미_다른_앨범에_사용된_경우_예외가_발생한다() throws Exception {
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
    class 앨범_초대_코드_생성_요청_시 {

        @Test
        void 유효한_요청이면_초대_코드_정보를_반환한다() throws Exception {
            // given
            Long albumId = 1L;

            InvitationLinkCreateResponse response =
                    new InvitationLinkCreateResponse(
                            "https://dev-api.cherrypic.today/participants/join?code=3FA7A9");

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
                                            "https://dev-api.cherrypic.today/participants/join?code=3FA7A9"));
        }
    }
}
