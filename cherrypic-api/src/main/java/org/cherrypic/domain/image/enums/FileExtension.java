package org.cherrypic.domain.image.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileExtension {
    PNG("png"),
    JPG("jpg"),
    JPEG("jpeg"),
    WEBP("webp"),
    HEIC("heic"),
    HEIF("heif"),

    MP4("mp4"),
    WEBM("webm"),
    MOV("mov"),
    MKV("mkv"),
    HEVC("hevc");

    private final String extension;

    @JsonCreator
    public static FileExtension from(String extension) {
        return Stream.of(values())
                .filter(e -> e.name().equalsIgnoreCase(extension))
                .findFirst()
                .orElse(null);
    }

    public static List<FileExtension> getImageExtensions() {
        return List.of(PNG, JPG, JPEG, WEBP, HEIC, HEIF);
    }

    public static List<FileExtension> getVideoExtensions() {
        return List.of(MP4, WEBM, MOV, MKV, HEVC);
    }
}
