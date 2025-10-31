package org.cherrypic.image.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.image.controller.ImageController;
import org.cherrypic.domain.image.dto.request.*;
import org.cherrypic.domain.image.dto.response.*;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.service.ImageService;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;
import org.cherrypic.s3.enums.FileExtension;
import org.cherrypic.tempalbum.enums.TempAlbumType;
import org.hamcrest.Matchers;
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
    class 프로필용_Presigned_URL_생성_요청_시 {

        @Test
        void 유효한_요청이면_회원_프로필_이미지용_Presigned_URL을_반환한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPEG, "testMd5Hash");

            PresignedUrlResponse response = new PresignedUrlResponse("testPresignedUrl");

            given(imageService.createMemberProfileImageUploadUrl(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/profile-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.presignedUrl").isNotEmpty());
        }

        @Test
        void 동영상_확장자를_입력할_경우_예외가_발생한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.MKV, "testMd5Hash");

            given(imageService.createMemberProfileImageUploadUrl(request))
                    .willThrow(new CustomException(ImageErrorCode.NOT_IMAGE_EXTENSION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/profile-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_IMAGE_EXTENSION"))
                    .andExpect(jsonPath("$.data.message").value("프로필과 커버에는 이미지 파일만 업로드 가능합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 이미지_파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            ImageUploadRequest request =
                    new ImageUploadRequest(FileExtension.from(extension), "testMd5Hash");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/profile-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시를_비워두면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPG, md5Hash);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/profile-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다,"));
        }
    }

    @Nested
    class 앨범_커버용_Presigned_URL_생성_요청_시 {

        @Test
        void 유효한_요청이면_앨범_커버_이미지용_Presigned_URL을_반환한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPEG, "testMd5Hash");

            PresignedUrlResponse response = new PresignedUrlResponse("testPresignedUrl");

            given(imageService.createAlbumCoverImageUploadUrl(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.presignedUrl").isNotEmpty());
        }

        @Test
        void 동영상_확장자를_입력할_경우_예외가_발생한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.MKV, "testMd5Hash");

            given(imageService.createAlbumCoverImageUploadUrl(request))
                    .willThrow(new CustomException(ImageErrorCode.NOT_IMAGE_EXTENSION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_IMAGE_EXTENSION"))
                    .andExpect(jsonPath("$.data.message").value("프로필과 커버에는 이미지 파일만 업로드 가능합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 이미지_파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            ImageUploadRequest request =
                    new ImageUploadRequest(FileExtension.from(extension), "testMd5Hash");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시를_비워두면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPG, md5Hash);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다,"));
        }
    }

    @Nested
    class 이벤트_커버용_Presigned_URL_생성_요청_시 {

        @Test
        void 유효한_요청이면_이벤트_커버_이미지용_Presigned_URL을_반환한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPEG, "testMd5Hash");

            PresignedUrlResponse response = new PresignedUrlResponse("testPresignedUrl");

            given(imageService.createEventCoverImageUploadUrl(request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.presignedUrl").isNotEmpty());
        }

        @Test
        void 동영상_확장자를_입력할_경우_예외가_발생한다() throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.MKV, "testMd5Hash");

            given(imageService.createEventCoverImageUploadUrl(request))
                    .willThrow(new CustomException(ImageErrorCode.NOT_IMAGE_EXTENSION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_IMAGE_EXTENSION"))
                    .andExpect(jsonPath("$.data.message").value("프로필과 커버에는 이미지 파일만 업로드 가능합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 이미지_파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            ImageUploadRequest request =
                    new ImageUploadRequest(FileExtension.from(extension), "testMd5Hash");

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시를_비워두면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            ImageUploadRequest request = new ImageUploadRequest(FileExtension.JPG, md5Hash);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/events/cover-upload-url")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다,"));
        }
    }

    @Nested
    class 앨범_이미지_업로드_Presigned_URL을_생성_요청_시 {

        @Test
        void 유효한_요청이면_이미지_업로드_Presigned_URL들을_반환한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            AlbumImageUploadResponse response =
                    new AlbumImageUploadResponse(List.of("testPresignedUrl1", "testPresignedUrl2"));

            given(imageService.createAlbumImageUploadUrls(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.urls").isNotEmpty());
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범에_속하지_않은_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void LIMITED_권한의_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.LIMITED_AUTHORITY));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Test
        void 구독이_만료된_앨범인_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION));

            // when & then
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("EXPIRED_SUBSCRIPTION"))
                    .andExpect(jsonPath("$.data.message").value("만료된 앨범에서는 요청을 처리할 수 없습니다."));
        }

        @Test
        void 앨범의_남은_용량을_초과해서_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash1", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_CAPACITY_EXCEEDED"))
                    .andExpect(jsonPath("$.data.message").value("앨범의 용량을 초과했습니다."));
        }

        @Test
        void MD5_해시에_중복된_값이_존재하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash", BigDecimal.ONE),
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash", BigDecimal.ONE)));

            given(imageService.createAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(ImageErrorCode.DUPLICATE_HASHES));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("DUPLICATE_HASHES"))
                    .andExpect(jsonPath("$.data.message").value("중복되는 md5 해시값이 존재합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.from(extension),
                                            "testMd5Hash1",
                                            BigDecimal.ONE)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "파일의 확장자는 비워둘 수 없으며, 이미지(PNG, JPG, JPEG, WEBP, HEIC, HEIF)와 동영상(MP4, WEBM, MOV, MKV, HEVC)만 지원됩니다."));
        }

        @Test
        void 이미지_용량을_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash", null)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("파일의 용량은 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시가_null_또는_공백이면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            AlbumImageUploadRequest request =
                    new AlbumImageUploadRequest(
                            List.of(
                                    new AlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, md5Hash, BigDecimal.ONE)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다."));
        }

        @Test
        void 업로드_요청_정보를_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest request = new AlbumImageUploadRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("업로드할 피일들의 정보는 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 앨범_이미지_목록_조회_요청시 {

        @Test
        void 정렬_파라미터가_UPLOAD이고_정렬_조건이_ASC이면_imageId를_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new AlbumImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(imageService.getAlbumImages(1L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_파라미터가_GENERATE이고_정렬_조건이_ASC이면_generatedAt을_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new AlbumImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(
                            imageService.getAlbumImages(
                                    1L, null, 2, SortParameter.GENERATE, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "2")
                                    .param("parameter", "GENERATE")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_파라미터가_UPLOAD이고_정렬_조건이_DESC면_imageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)),
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(
                            imageService.getAlbumImages(
                                    1L, null, 2, SortParameter.UPLOAD, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(2))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_파라미터가_GENERATE이고_정렬_조건이_DESC면_imageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)),
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(
                            imageService.getAlbumImages(
                                    1L, null, 2, SortParameter.GENERATE, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "2")
                                    .param("parameter", "GENERATE")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(2))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(imageService.getAlbumImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new AlbumImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(imageService.getAlbumImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 이미지가_없는_경우_빈_리스트를_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images = List.of();

            given(imageService.getAlbumImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            given(
                            imageService.getAlbumImages(
                                    999L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/999/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getAlbumImages(1L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images")
                                    .param("size", pageSize)
                                    .param("direction", "ASC"));

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
                            get("/albums/1/images").param("size", "1").param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
        }
    }

    @Nested
    class 이벤트_이미지_목록_조회_요청시 {

        @Test
        void 정렬_파라미터가_UPLOAD이고_정렬_조건이_ASC이면_eventImageId를_오름차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new EventImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(imageService.getEventImages(1L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_파라미터가_GENERATE이고_정렬_조건이_ASC이면_generatedAt을_오름차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new EventImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(
                            imageService.getEventImages(
                                    1L, null, 2, SortParameter.GENERATE, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "GENERATE")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
            ;
        }

        @Test
        void 정렬_파라미터가_UPLOAD이고_정렬_조건이_DESC면_eventImageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)),
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(
                            imageService.getEventImages(
                                    1L, null, 2, SortParameter.UPLOAD, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_파라미터가_GENERATE이고_정렬_조건이_DESC면_generatedAt를_내림차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)),
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(
                            imageService.getEventImages(
                                    1L, null, 2, SortParameter.GENERATE, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "GENERATE")
                                    .param("direction", "DESC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지인_경우_isLast를_true로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)));

            given(imageService.getEventImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 마지막_페이지가_아닌_경우_isLast를_false로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(
                                    1L, "testImageUrl1", LocalDateTime.of(2025, 1, 1, 0, 0)),
                            new EventImageListResponse(
                                    2L, "testImageUrl2", LocalDateTime.of(2025, 1, 2, 0, 0)));

            given(imageService.getEventImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(false));
        }

        @Test
        void 이벤트_이미지가_없는_경우_빈_리스트를_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages = List.of();

            given(imageService.getEventImages(1L, null, 1, SortParameter.UPLOAD, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "1")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 이벤트가_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getEventImages(1L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willThrow(new CustomException(EventErrorCode.EVENT_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getEventImages(1L, null, 2, SortParameter.UPLOAD, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", "2")
                                    .param("parameter", "UPLOAD")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images")
                                    .param("size", pageSize)
                                    .param("direction", "ASC"));

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
                            get("/events/1/images").param("size", "1").param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
        }
    }

    @Nested
    class 앨범_이미지_삭제_요청_시 {

        @Test
        void 유효한_요청이면_이미지를_삭제하고_NO_CONTENT를_반환한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of(1L, 2L));

            willDoNothing().given(imageService).deleteAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND))
                    .given(imageService)
                    .deleteAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범에_속하지_않은_사용자가_앨범_이미지를_삭제하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT))
                    .given(imageService)
                    .deleteAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void LIMITED_권한의_사용자가_앨범_이미지를_삭제하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.LIMITED_AUTHORITY))
                    .given(imageService)
                    .deleteAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Test
        void 앨범에_속하지_않은_이미지가_포함되어_있으면_예외가_발생한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(AlbumErrorCode.IMAGES_NOT_IN_ALBUM))
                    .given(imageService)
                    .deleteAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_NOT_IN_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속해 있지 않은 이미지가 포함되어 있습니다."));
        }

        @Test
        void 삭제하고자_하는_이미지_ID들을_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImageDeleteRequest request = new AlbumImageDeleteRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("삭제하고자 하는 이미지 ID들은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 임시_앨범_이미지_업로드_Presigned_URL을_생성_요청_시 {

        @Test
        void 유효한_요청이면_임시_앨범_이미지_업로드_Presigned_URL들을_반환한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            new BigDecimal("0.3")),
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            new BigDecimal("0.3"))));

            TempAlbumImageUploadListResponse response =
                    new TempAlbumImageUploadListResponse(
                            List.of(
                                    new TempAlbumImageUploadListResponse.Content(
                                            1L, "testPresignedUrl1"),
                                    new TempAlbumImageUploadListResponse.Content(
                                            2L, "testPresignedUrl2")));

            given(imageService.createTempAlbumImageUploadUrls(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isNotEmpty());
        }

        @Test
        void 임시_앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            new BigDecimal("0.3")),
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            new BigDecimal("0.3"))));

            given(imageService.createTempAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("TEMP_ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범이 존재하지 않습니다."));
        }

        @Test
        void 임시_앨범_소유자가_아닌_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            new BigDecimal("0.3")),
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            new BigDecimal("0.3"))));

            given(imageService.createTempAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_TEMP_ALBUM_OWNER"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 소유자가 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 임시_앨범의_남은_용량을_초과해서_요청하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            TempAlbumType.DEFAULT.getCapacityMb()),
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash2", BigDecimal.ONE)));

            given(imageService.createTempAlbumImageUploadUrls(1L, request))
                    .willThrow(
                            new CustomException(TempAlbumErrorCode.TEMP_ALBUM_CAPACITY_EXCEEDED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("TEMP_ALBUM_CAPACITY_EXCEEDED"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범의 용량을 초과했습니다."));
        }

        @Test
        void MD5_해시에_중복된_값이_존재하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            new BigDecimal("0.3")),
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            new BigDecimal("0.3"))));

            given(imageService.createTempAlbumImageUploadUrls(1L, request))
                    .willThrow(new CustomException(ImageErrorCode.DUPLICATE_HASHES));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("DUPLICATE_HASHES"))
                    .andExpect(jsonPath("$.data.message").value("중복되는 md5 해시값이 존재합니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {"JPEG1", "PDF", "TXT"})
        void 파일_확장자가_null_또는_지원하지_않는_형식이면_예외가_발생한다(String extension) throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.from(extension),
                                            "testMd5Hash1",
                                            BigDecimal.ONE)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value(
                                            "파일의 확장자는 비워둘 수 없으며, 이미지(PNG, JPG, JPEG, WEBP, HEIC, HEIF)와 동영상(MP4, WEBM, MOV, MKV, HEVC)만 지원됩니다."));
        }

        @Test
        void 이미지_용량을_비워두면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, "testMd5Hash", null)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("파일의 용량은 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시가_null_또는_공백이면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            TempAlbumImageUploadRequest request =
                    new TempAlbumImageUploadRequest(
                            List.of(
                                    new TempAlbumImageUploadRequest.Payload(
                                            FileExtension.JPEG, md5Hash, BigDecimal.ONE)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다."));
        }

        @Test
        void 업로드_요청_정보를_비워두면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageUploadRequest request = new TempAlbumImageUploadRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("업로드할 피일들의 정보는 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 임시_앨범_이미지_삭제_요청_시 {

        @Test
        void 유효한_요청이면_임시_앨범의_이미지를_삭제하고_NO_CONTENT를_반환한다() throws Exception {
            // given
            TempAlbumImageDeleteRequest request = new TempAlbumImageDeleteRequest(List.of(1L, 2L));

            willDoNothing().given(imageService).deleteTempAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 임시_앨범이_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageDeleteRequest request = new TempAlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND))
                    .given(imageService)
                    .deleteTempAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("TEMP_ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범이 존재하지 않습니다."));
        }

        @Test
        void 임시_앨범의_소유자가_아닌_사용자가_앨범_이미지를_삭제하면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageDeleteRequest request = new TempAlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER))
                    .given(imageService)
                    .deleteTempAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_TEMP_ALBUM_OWNER"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범 소유자가 아닌 경우 권한이 없습니다."));
        }

        @Test
        void 임시_앨범에_속하지_않은_이미지가_포함되어_있으면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageDeleteRequest request = new TempAlbumImageDeleteRequest(List.of(1L, 2L));

            willThrow(new CustomException(TempAlbumErrorCode.IMAGES_NOT_IN_TEMP_ALBUM))
                    .given(imageService)
                    .deleteTempAlbumImage(1L, request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGES_NOT_IN_TEMP_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("임시 앨범에 속해 있지 않은 이미지가 포함되어 있습니다."));
        }

        @Test
        void 삭제하고자_하는_임시_앨범의_이미지_ID들을_비워두면_예외가_발생한다() throws Exception {
            // given
            TempAlbumImageDeleteRequest request = new TempAlbumImageDeleteRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/temp-albums/1/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("삭제하고자 하는 임시 앨범 이미지 ID들은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 앨범_외_이미지_업로드_검증_요청_시 {

        @Test
        void 유효한_요청이면_NO_CONTENT를_반환한다() throws Exception {
            // given
            ImageConfirmRequest request = new ImageConfirmRequest("testImageUrl");

            willDoNothing().given(imageService).confirmNonAlbumImageUpload(request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/image/confirm-non-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 이미지_업로드_실패_시_예외가_발생한다() throws Exception {
            // given
            ImageConfirmRequest request = new ImageConfirmRequest("testImageUrl");

            willThrow(new CustomException(ImageErrorCode.IMAGE_UPLOAD_FAIL))
                    .given(imageService)
                    .confirmNonAlbumImageUpload(request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/image/confirm-non-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGE_UPLOAD_FAIL"))
                    .andExpect(jsonPath("$.data.message").value("이미지가 성공적으로 업로드 되지 못했습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 이미지_url을_비워두면_예외가_발생한다(String imageUrl) throws Exception {
            // given
            ImageConfirmRequest request = new ImageConfirmRequest(imageUrl);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/image/confirm-non-album")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이미지 url을 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 앨범_이미지_업로드_요청_시 {

        @Test
        void 유효한_요청이면_생성된_이미지_ID와_로컬_이미지_삭제_여부를_반환한다() throws Exception {
            // given
            AlbumImagesConfirmRequest request =
                    new AlbumImagesConfirmRequest(
                            List.of(
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl1"),
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl2")));

            AlbumImagesConfirmResponse response =
                    new AlbumImagesConfirmResponse(false, List.of(1L, 2L));

            given(imageService.confirmAlbumImagesUpload(1L, request)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.localImageDeletion").value(false))
                    .andExpect(jsonPath("$.data.imageIds").value(Matchers.contains(1, 2)));
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImagesConfirmRequest request =
                    new AlbumImagesConfirmRequest(
                            List.of(
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl1"),
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl2")));

            given(imageService.confirmAlbumImagesUpload(1L, request))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 모든_이미지를_업로드_성공하지_않았을_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImagesConfirmRequest request =
                    new AlbumImagesConfirmRequest(
                            List.of(
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl1"),
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, "testImageUrl2")));

            given(imageService.confirmAlbumImagesUpload(1L, request))
                    .willThrow(new CustomException(ImageErrorCode.IMAGE_UPLOAD_FAIL));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("IMAGE_UPLOAD_FAIL"))
                    .andExpect(jsonPath("$.data.message").value("이미지가 성공적으로 업로드 되지 못했습니다."));
        }

        @Test
        void 검증하고자_하는_이미지들의_정보를_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImagesConfirmRequest request = new AlbumImagesConfirmRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("검증하고자 하는 이미지들의 정보들은 비워둘 수 없습니다."));
        }

        @Test
        void 업로드_하는_파일의_용량을_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImagesConfirmRequest request =
                    new AlbumImagesConfirmRequest(
                            List.of(
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), null, "testImageUrl1")));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("파일의 용량은 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void 검증_요청_이미지_url을_비워두면_예외가_발생한다(String imageUrl) throws Exception {
            // given
            AlbumImagesConfirmRequest request =
                    new AlbumImagesConfirmRequest(
                            List.of(
                                    new AlbumImagesConfirmRequest.Payload(
                                            LocalDateTime.now(), BigDecimal.ONE, imageUrl)));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/confirm-images-upload")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("검증하고자 하는 imageUrl은 비워둘 수 없습니다."));
        }
    }
}
