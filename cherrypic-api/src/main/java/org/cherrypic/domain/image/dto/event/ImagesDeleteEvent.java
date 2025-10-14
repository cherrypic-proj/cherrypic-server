package org.cherrypic.domain.image.dto.event;

import java.util.List;

public record ImagesDeleteEvent(List<String> imageUrls) {
    public static ImagesDeleteEvent of(List<String> imageUrls) {
        return new ImagesDeleteEvent(imageUrls);
    }
}
