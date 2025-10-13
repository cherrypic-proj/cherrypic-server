package org.cherrypic.domain.tempalbum.job;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TempAlbumExpireJob {

    private final TempAlbumService tempAlbumService;

    public void run() {
        tempAlbumService.expireOverdueTempAlbum();
    }
}
