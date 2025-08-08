package org.cherrypic.domain.album.event;

import java.util.List;

public record AlbumDeleteEvent(Long albumId, Long senderId, List<Long> receiverIds) {
    public static AlbumDeleteEvent of(Long albumId, Long senderId, List<Long> receiverIds) {
        return new AlbumDeleteEvent(albumId, senderId, receiverIds);
    }
}
