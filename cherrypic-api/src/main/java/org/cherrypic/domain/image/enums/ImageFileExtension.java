package org.cherrypic.domain.image.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageFileExtension {
    PNG("png"),
    JPG("jpg"),
    JPEG("jpeg"),
    WEBP("webp"),
    HEIC("heic"),
    HEIF("heif");

    private final String extension;

    @JsonCreator
    public static ImageFileExtension from(String extension) {
        return Stream.of(values())
                .filter(e -> e.name().equalsIgnoreCase(extension))
                .findFirst()
                .orElse(null);
    }
}
