package org.cherrypic.payment.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.payment.controller.PaymentController;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.service.PaymentService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.payment.enums.PaymentPurpose;
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
                    .andExpect(jsonPath("$.data.message").value("BASIC 유형은 결제를 지원하지 않습니다."));
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
}
