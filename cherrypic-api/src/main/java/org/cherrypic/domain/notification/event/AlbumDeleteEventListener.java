package org.cherrypic.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.event.AlbumDeleteEvent;
import org.cherrypic.domain.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AlbumDeleteEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAlbumDeleteEvent(AlbumDeleteEvent event) {
        notificationService.sendAlbumDeleteNotification(
                event.albumId(), event.senderId(), event.receiverIds());
    }
}
