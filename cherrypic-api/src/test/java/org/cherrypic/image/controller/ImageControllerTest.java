package org.cherrypic.image.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.image.controller.ImageController;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.enums.ImageFileExtension;
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

    @Nested
    class 이미지_목록_조회_요청시 {

        @Test
        void 정렬_조건이_ASC이면_ImageId를_오름차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(1L, "testImageUrl1"),
                            new AlbumImageListResponse(2L, "testImageUrl2"));

            given(imageService.getAlbumImages(1L, 1L, null, 2, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.content[0].imageId").value(1))
                    .andExpect(jsonPath("$.data.content[1].imageId").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));
            ;
        }

        @Test
        void 정렬_조건이_DESC이면_imageId를_내림차순으로_응답한다() throws Exception {
            // given
            List<AlbumImageListResponse> images =
                    List.of(
                            new AlbumImageListResponse(2L, "testImageUrl2"),
                            new AlbumImageListResponse(1L, "testImageUrl1"));

            given(imageService.getAlbumImages(1L, 1L, null, 2, SortDirection.DESC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "2")
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
                    List.of(new AlbumImageListResponse(1L, "testImageUrl1"));

            given(imageService.getAlbumImages(1L, 1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "1")
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
                            new AlbumImageListResponse(1L, "testImageUrl1"),
                            new AlbumImageListResponse(2L, "testImageUrl2"));

            given(imageService.getAlbumImages(1L, 1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, false));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "1")
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

            given(imageService.getAlbumImages(1L, 1L, null, 1, SortDirection.ASC))
                    .willReturn(new SliceResponse<>(images, true));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "1")
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
            given(imageService.getAlbumImages(999L, null, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "999")
                                    .param("size", "2")
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
            given(imageService.getAlbumImages(1L, null, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.data.code").value("NOT_ALBUM_PARTICIPANT"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 속하지 않은 사용자입니다."));
        }

        @Test
        void 이벤트가_존재하지_않을_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getAlbumImages(1L, 1L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(EventErrorCode.EVENT_NOT_FOUND));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.message").value("존재하지 않는 이벤트입니다."));
        }

        @Test
        void 이벤트가_앨범에_속하지_않는_경우_예외가_발생한다() throws Exception {
            // given
            given(imageService.getAlbumImages(1L, 1L, null, 2, SortDirection.ASC))
                    .willThrow(new CustomException(EventErrorCode.EVENT_DOESNT_BELONG_TO_ALBUM));

            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "2")
                                    .param("direction", "ASC"));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("EVENT_DOESNT_BELONG_TO_ALBUM"))
                    .andExpect(jsonPath("$.data.message").value("앨범에 해당 이벤트가 존재하지 않습니다."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "-999", "0"})
        void 페이지_크기를_0_이하로_설정하면_예외가_발생한다(String pageSize) throws Exception {
            // when & then
            ResultActions perform =
                    mockMvc.perform(
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
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
                            get("/images")
                                    .param("albumId", "1")
                                    .param("eventId", "1")
                                    .param("size", "1")
                                    .param("direction", sort));

            perform.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.data.code").value("METHOD_ARGUMENT_TYPE_MISMATCH"))
                    .andExpect(jsonPath("$.data.message").value("요청한 값의 타입이 잘못되어 처리할 수 없습니다."));
        }
    }
}
