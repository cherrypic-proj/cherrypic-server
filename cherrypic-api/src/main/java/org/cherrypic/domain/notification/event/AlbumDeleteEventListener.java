package org.cherrypic.domain.notification.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.event.AlbumDeleteEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumDeleteEventListener {

    @EventListener
    public void handleAlbumDeleteEvent(AlbumDeleteEvent event) {}
}
