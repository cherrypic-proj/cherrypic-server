package org.cherrypic.s3.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageType {
    MEMBER_PROFILE("member-profile", "회원 프로필 사진"),
    ALBUM_COVER("album-cover", "앨범 커버"),
    ALBUM_IMAGE("album-image", "앨범 사진"),
    EVENT_COVER("event-cover", "이벤트 커버"),
    ;

    private final String type;
    private final String description;
}
