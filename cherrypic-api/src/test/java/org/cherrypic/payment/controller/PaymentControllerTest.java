package org.cherrypic.payment.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.payment.controller.PaymentController;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentListResponse;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.service.PaymentService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.payment.enums.PaymentPurpose;
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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PaymentService paymentService;

    @Nested
    class 결제_준비_요청_시 {

        @Nested
        class 유료_앨범_생성의_경우 {

            @Test
            void CREATION_목적의_결제_준비_정보를_반환한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, null);

                PaymentReadyResponse response =
                        new PaymentReadyResponse(
                                AlbumType.PRO,
                                3900,
                                "album_20250723_pro_1_a5c5dd8beaa6",
                                "상냥한 너구리",
                                PaymentPurpose.CREATION);

                given(paymentService.preparePayment(request)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                        .andExpect(jsonPath("$.data.type").value("PRO"))
                        .andExpect(jsonPath("$.data.price").value("3900"))
                        .andExpect(
                                jsonPath("$.data.merchantUid")
                                        .value("album_20250723_pro_1_a5c5dd8beaa6"))
                        .andExpect(jsonPath("$.data.buyerName").value("상냥한 너구리"))
                        .andExpect(jsonPath("$.data.purpose").value("CREATION"));
            }
        }

        @Nested
        class 구독_갱신의_경우 {

            @Test
            void RENEWAL_목적의_결제_준비_정보를_반환한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                PaymentReadyResponse response =
                        new PaymentReadyResponse(
                                AlbumType.PRO,
                                5900,
                                "album_20250723_pro_1_a5c5dd8beaa6",
                                "상냥한 너구리",
                                PaymentPurpose.RENEWAL);

                given(paymentService.preparePayment(request)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                        .andExpect(jsonPath("$.data.type").value("PRO"))
                        .andExpect(jsonPath("$.data.price").value("5900"))
                        .andExpect(
                                jsonPath("$.data.merchantUid")
                                        .value("album_20250723_pro_1_a5c5dd8beaa6"))
                        .andExpect(jsonPath("$.data.buyerName").value("상냥한 너구리"))
                        .andExpect(jsonPath("$.data.purpose").value("RENEWAL"));
            }

            @Test
            void 하위_앨범_유형으로_결제_준비를_요청하면_예외가_발생한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(PaymentErrorCode.DOWNGRADE_NOT_ALLOWED));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("DOWNGRADE_NOT_ALLOWED"))
                        .andExpect(
                                jsonPath("$.data.message")
                                        .value("현재 앨범 유형보다 낮은 유형으로 결제를 진행할 수 없습니다."));
            }

            @Test
            void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                        .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                        .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
            }
        }

        @Nested
        class 구독_업그레이드의_경우 {

            @Test
            void UPGRADE_목적의_결제_준비_정보를_반환한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PREMIUM, 1L);

                PaymentReadyResponse response =
                        new PaymentReadyResponse(
                                AlbumType.PREMIUM,
                                12900,
                                "album_20250723_pro_1_a5c5dd8beaa6",
                                "상냥한 너구리",
                                PaymentPurpose.UPGRADE);

                given(paymentService.preparePayment(request)).willReturn(response);

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                        .andExpect(jsonPath("$.data.type").value("PREMIUM"))
                        .andExpect(jsonPath("$.data.price").value("12900"))
                        .andExpect(
                                jsonPath("$.data.merchantUid")
                                        .value("album_20250723_pro_1_a5c5dd8beaa6"))
                        .andExpect(jsonPath("$.data.buyerName").value("상냥한 너구리"))
                        .andExpect(jsonPath("$.data.purpose").value("UPGRADE"));
            }

            @Test
            void 하위_앨범_유형으로_결제_준비를_요청하면_예외가_발생한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(PaymentErrorCode.DOWNGRADE_NOT_ALLOWED));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(jsonPath("$.data.code").value("DOWNGRADE_NOT_ALLOWED"))
                        .andExpect(
                                jsonPath("$.data.message")
                                        .value("현재 앨범 유형보다 낮은 유형으로 결제를 진행할 수 없습니다."));
            }

            @Test
            void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
                // given
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
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
                PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, 1L);

                given(paymentService.preparePayment(request))
                        .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

                // when & then
                ResultActions perform =
                        mockMvc.perform(
                                post("/payments/ready")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                perform.andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                        .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                        .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
            }
        }

        @Test
        void BASIC_유형으로_결제_준비를_요청하면_예외가_발생한다() throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.BASIC, null);

            given(paymentService.preparePayment(request))
                    .willThrow(new CustomException(PaymentErrorCode.UNSUPPORTED_PAYMENT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("UNSUPPORTED_PAYMENT"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 결제 기능을 지원하지 않습니다."));
        }

        @Test
        void 사용되지_않은_완료된_결제가_존재하면_예외가_발생한다() throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.PRO, null);

            given(paymentService.preparePayment(request))
                    .willThrow(
                            new CustomException(PaymentErrorCode.UNLINKED_PAYMENT_ALREADY_EXISTS));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("UNLINKED_PAYMENT_ALREADY_EXISTS"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("아직 사용되지 않은 완료된 결제 내역이 존재합니다. 앨범 생성 또는 구독을 먼저 완료해주세요."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" ", "PROO", "PREMIUMM"})
        void 앨범_유형이_null_또는_지원하지_않는_형식이면_예외가_발생한다(String type) throws Exception {
            // given
            PaymentReadyRequest request = new PaymentReadyRequest(AlbumType.from(type), null);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/payments/ready")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("유료 앨범 유형은 비워둘 수 없으며, PRO, PREMIUM만 지원됩니다."));
        }
    }

    @Nested
    class 결제_검증_요청_시 {

        @Test
        void 유효한_요청이면_결제_ID를_반환한다() throws Exception {
            // given
            PaymentVerificationResponse response = new PaymentVerificationResponse(1L);

            given(paymentService.verifyPayment("imp_1234")).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.paymentId").value("1"));
        }

        @Test
        void impUid가_아임포트에_존재하지_않으면_예외가_발생한다() throws Exception {
            // given
            given(paymentService.verifyPayment("imp_1234"))
                    .willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
        }

        @Test
        void merchantUid에_해당하는_결제가_DB에_없으면_예외가_발생한다() throws Exception {
            // given
            given(paymentService.verifyPayment("imp_1234"))
                    .willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
        }

        @Test
        void 결제_금액이_불일치하면_예외가_발생한다() throws Exception {
            // given
            given(paymentService.verifyPayment("imp_1234"))
                    .willThrow(new CustomException(PaymentErrorCode.AMOUNT_MISMATCH));

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("AMOUNT_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("결제 금액이 일치하지 않아 검증에 실패했습니다."));
        }

        @Test
        void 결제_상태가_PAID가_아니면_예외가_발생한다() throws Exception {
            // given
            given(paymentService.verifyPayment("imp_1234"))
                    .willThrow(new CustomException(PaymentErrorCode.NOT_PAID));

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_PAID"))
                    .andExpect(jsonPath("$.data.message").value("결제가 완료되지 않아 검증에 실패했습니다."));
        }

        @Test
        void Iamport_API_통신_장애가_발생하면_예외가_발생한다() throws Exception {
            // given
            given(paymentService.verifyPayment("imp_1234"))
                    .willThrow(new CustomException(PaymentErrorCode.IAMPORT_API_UNAVAILABLE));

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/verify/imp_1234"));

            perform.andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.SERVICE_UNAVAILABLE.value()))
                    .andExpect(jsonPath("$.data.code").value("IAMPORT_API_UNAVAILABLE"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("결제 대행 시스템(Iamport)과의 통신에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @Nested
    class 결제_취소_요청_시 {

        @Test
        void 유효한_요청이면_결제를_취소하고_NO_CONTENT_로_반환한다() throws Exception {
            // given
            willDoNothing().given(paymentService).cancelPayment("imp_1234");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void impUid가_아임포트에_존재하지_않으면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND))
                    .given(paymentService)
                    .cancelPayment("imp_9999");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
        }

        @Test
        void impUid에_해당하는_결제가_DB에_없으면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND))
                    .given(paymentService)
                    .cancelPayment("imp_9999");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("PAYMENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("결제 정보가 존재하지 않습니다."));
        }

        @Test
        void 이미_취소된_결제라면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(PaymentDomainErrorCode.ALREADY_CANCELED))
                    .given(paymentService)
                    .cancelPayment("imp_9999");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALREADY_CANCELED"))
                    .andExpect(jsonPath("$.data.message").value("해당 결제는 이미 취소되었습니다."));
        }

        @Test
        void 완료되지_않은_결제라면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(PaymentDomainErrorCode.ONLY_PAID_PAYMENT_CANCELABLE))
                    .given(paymentService)
                    .cancelPayment("imp_9999");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ONLY_PAID_PAYMENT_CANCELABLE"))
                    .andExpect(jsonPath("$.data.message").value("결제 취소는 완료된 결제만 가능합니다."));
        }

        @Test
        void Iamport_API_통신_장애가_발생하면_예외가_발생한다() throws Exception {
            // given
            willThrow(new CustomException(PaymentErrorCode.IAMPORT_API_UNAVAILABLE))
                    .given(paymentService)
                    .cancelPayment("imp_9999");

            // when & then
            ResultActions perform = mockMvc.perform(post("/payments/cancel/imp_9999"));

            perform.andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.SERVICE_UNAVAILABLE.value()))
                    .andExpect(jsonPath("$.data.code").value("IAMPORT_API_UNAVAILABLE"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("결제 대행 시스템(Iamport)과의 통신에 실패했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    @Nested
    class 앨범과_연결되지_않은_완료된_결제_내역_조회_요청_시 {

        @Test
        void 존재하면_1건의_결제_내역을_반환한다() throws Exception {
            // given
            PaymentUnlinkedResponse response =
                    new PaymentUnlinkedResponse(
                            1L,
                            AlbumType.PRO,
                            5900,
                            PaymentPurpose.RENEWAL,
                            LocalDateTime.of(2025, 8, 31, 20, 0));

            given(paymentService.getUnlinkedPayment()).willReturn(response);

            // when & then
            ResultActions perform = mockMvc.perform(get("/payments/unlinked"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.paymentId").value("1"))
                    .andExpect(jsonPath("$.data.albumType").value("PRO"))
                    .andExpect(jsonPath("$.data.amount").value("5900"))
                    .andExpect(jsonPath("$.data.purpose").value("RENEWAL"))
                    .andExpect(jsonPath("$.data.paidAt").value("2025-08-31T20:00:00"));
        }

        @Test
        void 존재하지_않으면_null을_반환한다() throws Exception {
            // given
            given(paymentService.getUnlinkedPayment()).willReturn(null);

            // when & then
            ResultActions perform = mockMvc.perform(get("/payments/unlinked"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data").value(nullValue()));
        }
    }

    @Nested
    class 앨범의_결제_내역_목록_조회_요청_시 {

        @Test
        void 정렬_조건이_ASC이면_paymentId를_오름차순으로_응답한다() throws Exception {
            // given
            List<PaymentListResponse> payments =
                    List.of(
                            new PaymentListResponse(1L, LocalDateTime.of(2025, 9, 2, 0, 0), 5900),
                            new PaymentListResponse(2L, LocalDateTime.of(2025, 10, 2, 0, 0), 5900));

            given(paymentService.getAlbumPayments(1L, null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(payments, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/payments")
                                    .param("albumId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].paymentId").value(1))
                    .andExpect(jsonPath("$.data.content[1].paymentId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_DESC이면_paymentId를_내림차순으로_응답한다() throws Exception {
            // given
            List<PaymentListResponse> payments =
                    List.of(
                            new PaymentListResponse(2L, LocalDateTime.of(2025, 10, 2, 0, 0), 5900),
                            new PaymentListResponse(1L, LocalDateTime.of(2025, 9, 2, 0, 0), 5900));

            given(paymentService.getAlbumPayments(1L, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(payments, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "2"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].paymentId").value(2))
                    .andExpect(jsonPath("$.data.content[1].paymentId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<PaymentListResponse> payments =
                    List.of(new PaymentListResponse(1L, LocalDateTime.of(2025, 9, 2, 0, 0), 5900));

            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(payments, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "1"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].paymentId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<PaymentListResponse> payments =
                    List.of(
                            new PaymentListResponse(2L, LocalDateTime.of(2025, 10, 2, 0, 0), 5900),
                            new PaymentListResponse(1L, LocalDateTime.of(2025, 9, 2, 0, 0), 5900));

            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(payments, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/payments")
                                    .param("albumId", "1")
                                    .param("size", "1")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].paymentId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "1"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 앨범_방장이_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_HOST));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "1"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_HOST"))
                    .andExpect(jsonPath("$.data.message").value("방장이 아닌 경우 권한이 없습니다."));
        }

        @Test
        void BASIC_유형인_경우_예외가_발생한다() throws Exception {
            // given
            given(paymentService.getAlbumPayments(1L, null, 1, SortDirection.DESC))
                    .willThrow(new CustomException(PaymentErrorCode.UNSUPPORTED_PAYMENT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", "1"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("UNSUPPORTED_PAYMENT"))
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 결제 기능을 지원하지 않습니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(get("/payments").param("albumId", "1").param("size", pageSize));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ConstraintViolationException"))
                    .andExpect(jsonPath("$.data.message").value("페이지 크기는 0보다 큰 값만 가능합니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ASCC", "DESCC", "OLDEST", "NEWEST"})
        void 존재하지_않는_정렬_기준을_입력한_경우_예외가_발생한다(String sort) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/payments")
                                    .param("albumId", "1")
                                    .param("size", "2")
                                    .param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
        }
    }
}
