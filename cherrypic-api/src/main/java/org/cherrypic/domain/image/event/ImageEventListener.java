package org.cherrypic.domain.image.event;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.event.AlbumImagesDeleteEvent;
import org.cherrypic.global.util.S3Util;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ImageEventListener {

    private final S3Util s3Util;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAlbumImagesDeleteEvent(AlbumImagesDeleteEvent event) {
        s3Util.deleteAllAlbumImagesInBatchFromS3(event.albumId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImagesDeleteEvent(ImagesDeleteEvent event) {
        s3Util.deleteFilesInBatchFromS3(event.imageUrls());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageDeleteEvent(ImageDeleteEvent event) {
        s3Util.deleteFileFromS3(event.imageUrl());
    }
}
