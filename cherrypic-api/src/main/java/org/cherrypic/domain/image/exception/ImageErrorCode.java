package org.cherrypic.domain.image.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {
    IMAGES_NOT_FOUND(404, "존재하지 않는 이미지를 포함하고 있습니다."),
    IMAGES_IN_OTHER_ALBUM(400, "앨범 소속이 아닌 이미지를 포함하고 있습니다."),
    IMAGE_DELETED(409, "이미지 관련 작업 중 이미지가 삭제되었습니다."),
    IMAGE_CONFLICT(409, "예상치 못한 이미지 무결성 오류"),

    PRESIGNED_IMAGES_NOT_MINE(403, "본인이 업로드하지 않은 Presigned Image는 삭제할 수 없습니다."),
    IMAGES_HASHES_SIZE_MISMATCH(400, "이미지 파일 확장자와 md5 개수가 일치하지 않습니다."),
    DUPLICATE_HASHES(400, "중복되는 md5 해시값이 존재합니다.");

    private final int status;
    private final String message;
}
