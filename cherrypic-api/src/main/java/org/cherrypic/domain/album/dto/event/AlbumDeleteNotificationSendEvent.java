package org.cherrypic.domain.album.dto.event;

import java.util.List;

public record AlbumDeleteNotificationSendEvent(
        Long albumId,
        Long senderId,
        String hostNickname,
        String albumTitle,
        List<Long> receiverIds) {
    public static AlbumDeleteNotificationSendEvent of(
            Long albumId,
            Long senderId,
            String hostNickname,
            String albumTitle,
            List<Long> receiverIds) {
        return new AlbumDeleteNotificationSendEvent(
                albumId, senderId, hostNickname, albumTitle, receiverIds);
    }
}
