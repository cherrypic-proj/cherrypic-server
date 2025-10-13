package org.cherrypic.domain.tempalbum.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.repository.TempAlbumImageRepository;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.s3.S3Util;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TempAlbumService {

    private final TempAlbumRepository tempAlbumRepository;
    private final TempAlbumImageRepository tempAlbumImageRepository;

    private final S3Util s3Util;

    public void expireOverdueTempAlbum() {
        List<TempAlbum> tempAlbums = tempAlbumRepository.findAllExpiredToday(LocalDate.now());
        List<Long> tempAlbumIds = tempAlbums.stream().map(TempAlbum::getId).toList();

        s3Util.deleteAllTempAlbumImagesInBatch(tempAlbumIds);
        tempAlbumImageRepository.deleteAllByTempAlbumIds(tempAlbumIds);
        tempAlbumRepository.deleteAllInBatch(tempAlbums);
    }
}
