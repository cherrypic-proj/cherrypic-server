package org.cherrypic.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.event.AlbumDeleteEvent;
import org.cherrypic.domain.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumDeleteEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleAlbumDeleteEvent(AlbumDeleteEvent event) {
        notificationService.sendAlbumDeleteNotification(
                event.albumId(), event.senderId(), event.receiverIds());
    }
}
