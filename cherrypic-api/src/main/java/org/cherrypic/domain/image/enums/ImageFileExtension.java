package org.cherrypic.domain.image.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.stream.Stream;
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

    @JsonCreator
    public static ImageFileExtension from(String extension) {
        return Stream.of(values())
                .filter(e -> e.name().equalsIgnoreCase(extension))
                .findFirst()
                .orElse(null);
    }

    public static List<ImageFileExtension> getPremiumAlbumImageFileExtension() {
        return null;
    }

    public static List<ImageFileExtension> getProAlbumImageFileExtension() {
        return null;
    }

    public static List<ImageFileExtension> getBasicAlbumImageFileExtension() {
        return null;
    }
}
