package org.cherrypic.domain.image.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageType {
    MEMBER_PROFILE("member_profile"),
    ALBUM_COVER("album_cover"),
    ALBUM_IMAGE("album_image"),
    EVENT_IMAGE("event_image"),
    ;

    private final String type;
}
