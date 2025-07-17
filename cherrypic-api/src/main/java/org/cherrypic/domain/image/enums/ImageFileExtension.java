package org.cherrypic.domain.image.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageFileExtension {
    PNG("png"),
    JPG("jpg"),
    JPEG("jpeg"),
    ;

    private final String extension;
}
