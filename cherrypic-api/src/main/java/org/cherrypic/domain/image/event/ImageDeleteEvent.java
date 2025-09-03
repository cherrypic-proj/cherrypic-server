package org.cherrypic.domain.image.event;

public record ImageDeleteEvent(String imageUrl) {
    public static ImageDeleteEvent of(String imageUrl) {
        return new ImageDeleteEvent(imageUrl);
    }
}
