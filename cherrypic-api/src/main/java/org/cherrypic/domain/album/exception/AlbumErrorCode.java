package org.cherrypic.domain.album.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum AlbumErrorCode implements BaseErrorCode {
    ALBUM_NOT_FOUND(404, "앨범이 존재하지 않습니다."),
    ALBUM_HOST_NOT_FOUND(404, "방장이 존재하지 않는 앨범입니다"),
    NOT_ALBUM_HOST(403, "방장이 아닌 경우 권한이 없습니다."),
    NOT_ALBUM_PARTICIPANT(403, "앨범에 속하지 않은 사용자입니다."),
    LIMITED_AUTHORITY(403, "앨범에 대한 생성/수정 권한이 없습니다."),

    PERMISSION_CONTROL_NOT_ALLOWED_FOR_BASIC_PLAN(400, "BASIC 플랜에서는 권한 부여 활성화가 허용되지 않습니다."),

    PAYMENT_REQUIRED_FOR_PAID_PLAN(400, "유료 플랜은 결제 ID가 필요합니다."),
    PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN(400, "BASIC 플랜에서는 결제 ID가 필요하지 않습니다."),

    INVITATION_CODE_NOT_FOUND(404, "앨범의 초대 코드가 만료되었습니다."),
    INVITATION_CODE_MISMATCH(400, "초대 코드가 올바르지 않습니다."),
    ALREADY_PARTICIPATED(400, "이미 참가한 앨범입니다."),
    ALBUM_PARTICIPANT_LIMIT_EXCEEDED(400, "앨범 인원 제한으로 더 이상 참가할 수 없습니다."),

    OTHER_PARTICIPANTS_EXIST(400, "다른 참가자가 남아 있어 앨범을 삭제할 수 없습니다."),
    SUBSCRIPTION_ACTIVE(400, "구독 중인 앨범은 삭제할 수 없습니다."),

    HOST_LEAVE_NOT_ALLOWED(403, "방장은 앨범을 나갈 수 없습니다."),
    HOST_SELF_KICK_NOT_ALLOWED(400, "방장은 자기 자신을 강퇴할 수 없습니다."),

    HOST_SELF_ROLE_CHANGE_NOT_ALLOWED(400, "방장은 자기 자신의 권한을 직접 변경할 수 없습니다."),

    PARTICIPANT_NOT_IN_ALBUM(400, "해당 참가자는 이 앨범에 속해 있지 않습니다."),

    SUBSCRIPTION_ACTIVE_HOST_TRANSFER_NOT_ALLOWED(400, "구독 중인 앨범에서는 방장 권한을 넘길 수 없습니다."),

    ALBUM_CAPACITY_EXCEEDED(400, "앨범의 용량을 초과했습니다."),
    IMAGES_NOT_IN_ALBUM(400, "앨범에 속해 있지 않은 이미지가 포함되어 있습니다.");
    ;

    private final int status;
    private final String message;
}
