package org.cherrypic.image.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cherrypic.domain.image.controller.ImageController;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.domain.image.service.ImageService;
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

@WebMvcTest(ImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImageControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ImageService imageService;

    @Nested
    class Presigned_URL을_생성할_때 {

        @Test
        void 유효한_요청이면_회원_프로필_이미지용_Presigned_URL을_반환한다() throws Exception {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(ImageFileExtension.JPEG);

            PresignedUrlResponse response = new PresignedUrlResponse("testPresignedUrl");

            given(imageService.createMemberProfileImageUploadUrl(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/me/upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.presignedUrl").isNotEmpty());
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 이미지_파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(ImageFileExtension.from(extension));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/me/upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG만 지원됩니다."));
        }
    }
}
