package org.cherrypic.album.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.controller.AlbumController;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.payment.exception.PaymentDomainErrorCode;
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
        class BASIC_유형인_경우 {

            @Test
            void 결제ID_없이_요청하면_앨범_생성_정보를_반환한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, null, false);

                AlbumCreateResponse response =
                        new AlbumCreateResponse(
                                1L, "testTitle", "testCoverUrl", AlbumType.BASIC, false);

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
                        .andExpect(jsonPath("$.data.type").value("BASIC"))
                        .andExpect(jsonPath("$.data.permissionControl").value("false"));
            }

            @Test
            void 결제ID를_포함하여_요청하면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, 1L, false);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new CustomException(
                                        AlbumErrorCode.PAYMENT_NOT_REQUIRED_FOR_BASIC_TYPE));

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
                                        .value("PAYMENT_NOT_REQUIRED_FOR_BASIC_TYPE"))
                        .andExpect(
                                jsonPath("$.data.message").value("BASIC 유형에서는 결제 ID가 필요하지 않습니다."));
            }

            @Test
            void 권한_부여_활성화_여부를_true로_요청하면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.BASIC, null, true);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new CustomException(
                                        AlbumErrorCode
                                                .PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE));

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
                                        .value("PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE"))
                        .andExpect(
                                jsonPath("$.data.message")
                                        .value("BASIC 유형에서는 권한 부여 활성화가 허용되지 않습니다."));
            }
        }

        @Nested
        class PRO_또는_PREMIUM_유형인_경우 {

            @Test
            void 유효한_결제ID면_앨범_생성_정보를_반환한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, true);

                AlbumCreateResponse response =
                        new AlbumCreateResponse(
                                1L, "testTitle", "testCoverUrl", AlbumType.PRO, true);

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
                        .andExpect(jsonPath("$.data.type").value("PRO"))
                        .andExpect(jsonPath("$.data.permissionControl").value("true"));
            }

            @Test
            void 결제ID가_null이면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, null, false);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new CustomException(AlbumErrorCode.PAYMENT_REQUIRED_FOR_PAID_TYPE));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("PAYMENT_REQUIRED_FOR_PAID_TYPE"))
                        .andExpect(jsonPath("$.data.message").value("유료 앨범 유형은 결제 ID가 필요합니다."));
            }

            @Test
            void 존재하지_않는_결제ID면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 999L, false);

                given(albumService.createAlbum(request))
                        .willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

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
            void 결제한_회원과_로그인_회원이_일치하지_않으면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, false);

                given(albumService.createAlbum(request))
                        .willThrow(new CustomException(PaymentErrorCode.PAYMENT_MEMBER_MISMATCH));

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
            void 결제상태가_PAID가_아니면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, false);

                given(albumService.createAlbum(request))
                        .willThrow(new CustomException(PaymentDomainErrorCode.NOT_PAID));

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
                        .andExpect(jsonPath("$.data.message").value("아직 결제가 완료되지 않았습니다."));
            }

            @Test
            void 결제가_이미_사용된_경우_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, false);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new CustomException(PaymentDomainErrorCode.ALREADY_USED_PAYMENT));

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
                        .andExpect(jsonPath("$.data.message").value("해당 결제는 이미 사용되었습니다."));
            }

            @Test
            void 결제의_목적이_앨범_생성과_일치하지_않으면_예외가_발생한다() throws Exception {
                // given
                AlbumCreateRequest request =
                        new AlbumCreateRequest(
                                "testTitle", "testCoverUrl", AlbumType.PRO, 1L, false);

                given(albumService.createAlbum(request))
                        .willThrow(
                                new CustomException(
                                        PaymentDomainErrorCode.PAYMENT_PURPOSE_MISMATCH));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/albums")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("PAYMENT_PURPOSE_MISMATCH"))
                        .andExpect(jsonPath("$.data.message").value("결제 목적이 요청하려는 작업과 일치하지 않습니다."));
            }
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 앨범_이름이_null_또는_공백이면_예외가_발생한다(String title) throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest(title, "testCoverUrl", AlbumType.BASIC, null, false);

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
                    new AlbumCreateRequest(
                            "t".repeat(21), "testCoverUrl", AlbumType.BASIC, null, false);

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
        void 앨범_유형이_null_또는_지원하지_않는_형식이면_예외가_발생한다(String type) throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest(
                            "testTitle", "testCoverUrl", AlbumType.from(type), 1L, false);

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
                                    .value("앨범 유형은 비워둘 수 없으며, BASIC, PRO, PREMIUM만 지원됩니다."));
        }

        @ParameterizedTest
        @NullSource
        void 권한_부여_활성화_여부가_null이면_예외가_발생한다(Boolean permissionControl) throws Exception {
            // given
            AlbumCreateRequest request =
                    new AlbumCreateRequest(
                            "testTitle", "testCoverUrl", AlbumType.BASIC, 1L, permissionControl);

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
                    .andExpect(jsonPath("$.data.message").value("권한 부여 활성화 여부는 비워둘 수 없습니다."));
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
                            1L, "testUpdatedTitle", "testUpdatedCoverUrl", AlbumType.BASIC);

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
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

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
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

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
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

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

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            AlbumUpdateRequest request = new AlbumUpdateRequest("testTitle", "testCoverUrl");

            given(albumService.updateAlbum(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            patch("/albums/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
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
    class 멤버별_권한_부여_상태_변경_요청_시 {

        @Test
        void 유효한_요청이면_권한_부여_토글_상태를_반환한다() throws Exception {
            // given
            PermissionToggleResponse response = new PermissionToggleResponse(true);

            given(albumService.togglePermission(1L)).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.permissionControl").value(true));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.togglePermission(1L))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.togglePermission(1L))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.togglePermission(1L))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.togglePermission(1L))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.togglePermission(1L))
                    .willThrow(
                            new CustomException(
                                    AlbumErrorCode.PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE));

            // when & then
            ResultActions perform = mockMvc.perform(patch("/albums/1/permission"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(
                            jsonPath("$.data.code")
                                    .value("PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_TYPE"))
                    .andExpect(
                            jsonPath("$.data.message").value("BASIC 유형에서는 권한 부여 활성화가 허용되지 않습니다."));
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
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

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
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

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
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform = mockMvc.perform(post("/albums/1/invitation-link"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.createInvitationLink(1L))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            ResultActions perform = mockMvc.perform(post("/albums/1/invitation-link"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
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
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

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
            given(albumService.joinAlbum(1L, "noneExistingCode"))
                    .willThrow(new CustomException(AlbumErrorCode.INVITATION_CODE_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "noneExistingCode"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("INVITATION_CODE_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범의 초대 코드가 만료되었습니다."));
        }

        @Test
        void 앨범_초대_코드가_redis에_저장된_코드와_일치하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "testInvitationCode"))
                    .willThrow(new CustomException(AlbumErrorCode.ALREADY_PARTICIPATED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "testInvitationCode"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_PARTICIPATED"))
                    .andExpect(jsonPath("$.data.message").value("이미 참가한 앨범입니다."));
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "expiredInvitationCode"))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "expiredInvitationCode"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
        }

        @Test
        void 이미_입장한_앨범에_재입장_하려는_경우_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "expiredInvitationCode"))
                    .willThrow(new CustomException(AlbumErrorCode.INVITATION_CODE_MISMATCH));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "expiredInvitationCode"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("INVITATION_CODE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("초대 코드가 올바르지 않습니다."));
        }

        @Test
        void 최대_참가자_수를_초과하면_예외가_발생한다() throws Exception {
            // given
            given(albumService.joinAlbum(1L, "testInvitationCode"))
                    .willThrow(
                            new CustomException(AlbumErrorCode.ALBUM_PARTICIPANT_LIMIT_EXCEEDED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(post("/albums/1/join").param("code", "testInvitationCode"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_PARTICIPANT_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.data.message").value("앨범 인원 제한으로 더 이상 참가할 수 없습니다."));
        }
    }

    @Nested
    class 개별_앨범_조회_요청_시 {

        @Test
        void 유효한_요청인_경우_앨범_정보를_반환한다() throws Exception {
            // given
            AlbumInfoResponse response =
                    new AlbumInfoResponse(
                            "testAlbum",
                            "testUrl",
                            AlbumType.BASIC,
                            new BigDecimal("0.00"),
                            new BigDecimal("3"),
                            "testNickname",
                            1);
            given(albumService.getAlbum(1L)).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.title").value("testAlbum"))
                    .andExpect(jsonPath("$.data.coverUrl").value("testUrl"))
                    .andExpect(jsonPath("$.data.type").value("BASIC"))
                    .andExpect(jsonPath("$.data.capacityUsed").value("0.00"))
                    .andExpect(jsonPath("$.data.totalCapacity").value("3"))
                    .andExpect(jsonPath("$.data.hostName").value("testNickname"))
                    .andExpect(jsonPath("$.data.numOfParticipants").value(1));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(albumService)
                    .getAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참여자가_아닌_경우_에외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(albumService)
                    .getAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범에_방장이_없는_경우_에외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_HOST_NOT_FOUND))
                    .given(albumService)
                    .getAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(get("/albums/1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_HOST_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("방장이 존재하지 않는 앨범입니다"));
        }
    }

    @Nested
    class 앨범_목록_조회_요청_시 {

        @Test
        void PRO_유형으로_필터링하면_PRO_앨범만_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    true),
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    AlbumType.PRO, null, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("type", "PRO").param("size", "2"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].type").value("PRO"))
                    .andExpect(jsonPath("$.data.content[1].type").value("PRO"))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 앨범_이름으로_필터링하면_일치하는_앨범만_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    true),
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, "testTitle", null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums").param("keyword", "testTitle").param("size", "2"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].title").value("testTitle2"))
                    .andExpect(jsonPath("$.data.content[1].title").value("testTitle1"))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void PRO_유형과_앨범_이름으로_필터링하면_조건을_모두_만족하는_앨범만_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    true),
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    AlbumType.PRO, "testTitle", null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(albums, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums")
                                    .param("type", "PRO")
                                    .param("keyword", "testTitle")
                                    .param("size", "2"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].title").value("testTitle2"))
                    .andExpect(jsonPath("$.data.content[1].title").value("testTitle1"))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_ASC이면_albumId를_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumListResponse> albums =
                    List.of(
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    false),
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    true));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, null, null, 2, SortDirection.ASC))
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
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    true),
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, null, null, 2, SortDirection.DESC))
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
                    List.of(
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, null, null, 1, SortDirection.DESC))
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
                            new AlbumListResponse(
                                    2L,
                                    "testTitle2",
                                    "testCoverUrl2",
                                    AlbumType.PRO,
                                    AlbumType.PRO.getPrice(),
                                    LocalDateTime.now(),
                                    true),
                            new AlbumListResponse(
                                    1L,
                                    "testTitle1",
                                    "testCoverUrl1",
                                    AlbumType.BASIC,
                                    AlbumType.BASIC.getPrice(),
                                    LocalDateTime.now(),
                                    false));

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, null, null, 1, SortDirection.DESC))
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

            given(
                            albumService.getParticipatingAlbumsByCondition(
                                    null, null, null, 10, SortDirection.DESC))
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

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기가_0_이하이면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums").param("size", pageSize).param("direction", "DESC"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ConstraintViolationException"))
                    .andExpect(jsonPath("$.data.message").value("페이지 크기는 0보다 큰 값만 가능합니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ASCC", "DESCC", "OLDEST", "NEWEST"})
        void 존재하지_않는_정렬_기준을_입력하면_예외가_발생한다(String sort) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/albums").param("size", "2").param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
        }
    }

    @Nested
    class 앨범_삭제_요청_시 {

        @Test
        void 유효한_요청이면_앨범을_삭제하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            willDoNothing().given(albumService).deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(albumService)
                    .deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(albumService)
                    .deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST))
                    .given(albumService)
                    .deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 다른_참가자가_남아있는_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.OTHER_PARTICIPANTS_EXIST))
                    .given(albumService)
                    .deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("OTHER_PARTICIPANTS_EXIST"))
                    .andExpect(jsonPath("$.data.message").value("다른 참가자가 남아 있어 앨범을 삭제할 수 없습니다."));
        }

        @Test
        void 구독_중인_경우_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(AlbumErrorCode.SUBSCRIPTION_ACTIVE))
                    .given(albumService)
                    .deleteAlbum(1L);

            // when & then
            ResultActions perform = mockMvc.perform(delete("/albums/1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("SUBSCRIPTION_ACTIVE"))
                    .andExpect(jsonPath("$.data.message").value("구독 중인 앨범은 삭제할 수 없습니다."));
        }
    }
}
