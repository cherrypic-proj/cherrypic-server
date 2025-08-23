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
import org.cherrypic.domain.image.dto.request.AlbumImageUploadRequest;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.request.UploadFailedImageDeleteRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlsResponse;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.service.ImageService;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
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
    class Presigned_URL을_생성_요청_시 {

        @Test
        void 유효한_요청이면_회원_프로필_이미지용_Presigned_URL을_반환한다() throws Exception {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(FileExtension.JPEG, "testMd5Hash");

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

        @Test
        void 동영상_확장자를_입력할_경우_예외가_발생한다() throws Exception {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(FileExtension.MKV, "testMd5Hash");

            given(imageService.createMemberProfileImageUploadUrl(request))
                    .willThrow(new CustomException(ImageErrorCode.NOT_IMAGE_EXTENSION));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/members/me/upload-url")
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
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(
                            FileExtension.from(extension), "testMd5Hash");

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
                                    .value(
                                            "이미지 파일의 확장자는 비워둘 수 없으며, PNG, JPG, JPEG, WEBP, HEIC, HEIF만 지원됩니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시를_비워두면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            MemberProfileImageUploadRequest request =
                    new MemberProfileImageUploadRequest(FileExtension.from("JPG"), md5Hash);

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
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다,"));
        }
    }

    @Nested
    class 앨범_이미지_업로드_Presigned_URL을_생성_요청_시 {

        @Test
        void 유효한_요청이면_이미지_업로드_Presigned_URL들을_반환한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            PresignedUrlsResponse response =
                    new PresignedUrlsResponse(List.of("testPresignedUrl1", "testPresignedUrl2"));

            given(imageService.createAlbumImageUploadUrls(1L, requests)).willReturn(response);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.presignedUrls").isNotEmpty());
        }

        @Test
        void 앨범이_존재하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            given(imageService.createAlbumImageUploadUrls(1L, requests))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범에_속하지_않은_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            given(imageService.createAlbumImageUploadUrls(1L, requests))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void LIMITED_권한의_사용자가_앨범_이미지_업로드_URL을_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            given(imageService.createAlbumImageUploadUrls(1L, requests))
                    .willThrow(new CustomException(AlbumErrorCode.LIMITED_AUTHORITY));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("LIMITED_AUTHORITY"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 대한 생성/수정 권한이 없습니다."));
        }

        @Test
        void 앨범의_남은_용량을_초과해서_요청하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash1",
                                            LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash2",
                                            LocalDateTime.now())));

            given(imageService.createAlbumImageUploadUrls(1L, requests))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_CAPACITY_EXCEEDED"))
                    .andExpect(jsonPath("$.data.message").value("앨범의 용량을 초과했습니다."));
        }

        @Test
        void MD5_해시에_중복된_값이_존재하면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG, "testMd5Hash", LocalDateTime.now()),
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash",
                                            LocalDateTime.now())));

            given(imageService.createAlbumImageUploadUrls(1L, requests))
                    .willThrow(new CustomException(ImageErrorCode.DUPLICATE_HASHES));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

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
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.from(extension),
                                            "testMd5Hash1",
                                            LocalDateTime.now())));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

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
        void 이미지_용량_총합을_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            null,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG,
                                            "testMd5Hash",
                                            LocalDateTime.now())));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("이미지 파일들의 용량은 비워둘 수 없습니다."));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        @ValueSource(strings = {" "})
        void MD5_해시가_null_또는_공백이면_예외가_발생한다(String md5Hash) throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(
                            BigDecimal.ONE,
                            List.of(
                                    new AlbumImageUploadRequest.payload(
                                            FileExtension.JPEG, md5Hash, LocalDateTime.now())));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("MD5 해시값은 비워둘 수 없습니다."));
        }

        @Test
        void 업로드_요청_정보를_비워두면_예외가_발생한다() throws Exception {
            // given
            AlbumImageUploadRequest requests =
                    new AlbumImageUploadRequest(BigDecimal.ONE, List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            post("/albums/1/image-upload-urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(requests)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("앨범 이미지 업로드 요청은 비워둘 수 없습니다."));
        }
    }

    @Nested
    class 앨범_이미지_목록_조회_요청시 {

        @Test
        void 정렬_조건이_ASC이면_imageId를_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(1L, "testImageUrl1"),
                            new AlbumImageListResponse(2L, "testImageUrl2"));

            given(imageService.getAlbumImages(1L, null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "2").param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 정렬_조건이_DESC이면_imageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(2L, "testImageUrl2"),
                            new AlbumImageListResponse(1L, "testImageUrl1"));

            given(imageService.getAlbumImages(1L, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "2").param("direction", "DESC"));

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
                    List.of(new AlbumImageListResponse(1L, "testImageUrl1"));

            given(imageService.getAlbumImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "1").param("direction", "ASC"));

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
                            new AlbumImageListResponse(1L, "testImageUrl1"),
                            new AlbumImageListResponse(2L, "testImageUrl2"));

            given(imageService.getAlbumImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "1").param("direction", "ASC"));

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

            given(imageService.getAlbumImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "1").param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 앨범이_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getAlbumImages(999L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/999/images").param("size", "2").param("direction", "ASC"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("ALBUM_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("앨범이 존재하지 않습니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getAlbumImages(1L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/albums/1/images").param("size", "2").param("direction", "ASC"));

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
        void 정렬_조건이_ASC이면_eventImageId를_오름차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(1L, "testImageUrl1"),
                            new EventImageListResponse(2L, "testImageUrl2"));

            given(imageService.getEventImages(1L, null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "2").param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].eventImageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].eventImageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
            ;
        }

        @Test
        void 정렬_조건이_DESC이면_eventImageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<EventImageListResponse> eventImages =
                    List.of(
                            new EventImageListResponse(2L, "testImageUrl2"),
                            new EventImageListResponse(1L, "testImageUrl1"));

            given(imageService.getEventImages(1L, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "2").param("direction", "DESC"));

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
                    List.of(new EventImageListResponse(1L, "testImageUrl1"));

            given(imageService.getEventImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "1").param("direction", "ASC"));

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
                            new EventImageListResponse(1L, "testImageUrl1"),
                            new EventImageListResponse(2L, "testImageUrl2"));

            given(imageService.getEventImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "1").param("direction", "ASC"));

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

            given(imageService.getEventImages(1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(eventImages, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "1").param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.isLast").value(true));
        }

        @Test
        void 이벤트가_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getEventImages(1L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(EventErrorCode.EVENT_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "2").param("direction", "ASC"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
        }

        @Test
        void 앨범_참가자가_아닌_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getEventImages(1L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/events/1/images").param("size", "2").param("direction", "ASC"));

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
    class 업로드_실패한_이미지_삭제_요청_시 {

        @Test
        void 유효한_요청이면_이미지를_삭제하고_NO_CONTENT를_반환한다() throws Exception {
            // given
            UploadFailedImageDeleteRequest request =
                    new UploadFailedImageDeleteRequest(List.of("testPresignedUrl"));

            willDoNothing().given(imageService).deleteUploadFailedImages(request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NO_CONTENT.value()));
        }

        @Test
        void 내가_업로드하지_않은_이미지를_삭제할_경우_예외가_발생한다() throws Exception {
            // given
            UploadFailedImageDeleteRequest request =
                    new UploadFailedImageDeleteRequest(List.of("testPresignedUrl"));
            willThrow(new CustomException(ImageErrorCode.PRESIGNED_IMAGES_NOT_MINE))
                    .given(imageService)
                    .deleteUploadFailedImages(request);

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("PRESIGNED_IMAGES_NOT_MINE"))
                    .andExpect(
                            jsonPath("$.data.message")
                                    .value("본인이 업로드하지 않은 Presigned Image는 삭제할 수 없습니다."));
        }

        @Test
        void Presigned_URL이_없는_경우_예외가_발생한다() throws Exception {
            // given
            UploadFailedImageDeleteRequest request = new UploadFailedImageDeleteRequest(List.of());

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            delete("/images")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("MethodArgumentNotValidException"))
                    .andExpect(jsonPath("$.data.message").value("Presigned URL은 비워둘 수 없습니다."));
        }
    }
}
