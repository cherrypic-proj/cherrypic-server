package org.cherrypic.domain.tempalbum.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.tempalbum.TempAlbum;
import org.cherrypic.tempalbum.TempAlbumImage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TempAlbumServiceImpl implements TempAlbumService {

    private final MemberUtil memberUtil;

    private final ParticipantRepository participantRepository;
    private final ImageRepository imageRepository;
    private final AlbumRepository albumRepository;
    private final TempAlbumRepository tempAlbumRepository;

    @Override
    public void createTempAlbum(Long albumId, TempAlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateParticipantAuthority(currentMember.getId(), album.getId());

        List<Long> distinctImageIds =
                request.imageIds().stream().filter(Objects::nonNull).distinct().toList();
        final List<Image> images = imageRepository.findAllById(distinctImageIds);

        validateImagesInAlbum(images, album);

        TempAlbum tempAlbum =
                TempAlbum.createTempAlbum(
                        album, currentMember, getExpirationByAlbumPlan(album.getPlan()));

        List<TempAlbumImage> tempAlbumImages =
                images.stream()
                        .map(image -> TempAlbumImage.createTempAlbumImage(tempAlbum, image))
                        .toList();

        tempAlbum.addTempAlbumImages(tempAlbumImages);
        tempAlbumRepository.save(tempAlbum);
    }

    private Date getExpirationByAlbumPlan(AlbumPlan albumPlan) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();

        if (AlbumPlan.BASIC == albumPlan) {
            expTimeMillis += TimeUnit.DAYS.toMillis(3);
        } else {
            expTimeMillis += TimeUnit.DAYS.toMillis(14);
        }

        expiration.setTime(expTimeMillis);
        return expiration;
    }

    private void validateParticipantAuthority(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (participant.getRole().equals(ParticipantRole.LIMITED)) {
            throw new CustomException(AlbumErrorCode.LIMITED_AUTHORITY);
        }
    }

    private void validateImagesInAlbum(List<Image> images, Album album) {
        boolean containsNotInAlbum =
                images.stream().anyMatch(ei -> !ei.getAlbum().getId().equals(album.getId()));

        if (containsNotInAlbum) {
            throw new CustomException(AlbumErrorCode.IMAGES_NOT_IN_ALBUM);
        }
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }
}
