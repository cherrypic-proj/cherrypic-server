package org.cherrypic.domain.album.event;

public record AlbumImageBatchDeleteEvent(Long albumId) {
    public static AlbumImageBatchDeleteEvent of(Long albumId) {
        return new AlbumImageBatchDeleteEvent(albumId);
    }
}
