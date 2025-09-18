package org.cherrypic.domain.image.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.*;
import org.cherrypic.domain.image.dto.response.*;
import org.cherrypic.domain.image.event.ImagesDeleteEvent;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.tempalbum.event.TempAlbumImagesDeleteEvent;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.repository.TempAlbumImageRepository;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.s3.S3Util;
import org.cherrypic.s3.enums.FileExtension;
import org.cherrypic.s3.enums.ImageType;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.cherrypic.tempalbum.entity.TempAlbumImage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {

    private final MemberUtil memberUtil;
    private final S3Util s3Util;

    private final AlbumRepository albumRepository;
    private final ImageRepository imageRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final TempAlbumRepository tempAlbumRepository;
    private final TempAlbumImageRepository tempAlbumImageRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PresignedUrlResponse createMemberProfileImageUploadUrl(ImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.MEMBER_PROFILE,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return PresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public PresignedUrlResponse createAlbumCoverImageUploadUrl(ImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.ALBUM_COVER,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return PresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public PresignedUrlResponse createEventCoverImageUploadUrl(ImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.EVENT_COVER,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return PresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public ImageUploadListResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumByIdWithLock(albumId);

        validateParticipantAuthority(currentMember.getId(), album.getId());
        validateSubscriptionNotExpired(album);

        BigDecimal uploadCapacity =
                request.payloads().stream()
                        .map(AlbumImageUploadRequest.Payload::capacity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateAlbumCapacity(album, uploadCapacity);
        validateDistinctHashes(request);

        album.increaseCapacity(uploadCapacity);

        List<String> presignedUrls =
                request.payloads().stream()
                        .map(
                                req ->
                                        s3Util.createPresignedUrl(
                                                ImageType.ALBUM_IMAGE,
                                                album.getId(),
                                                req.fileExtension(),
                                                req.md5Hashes()))
                        .toList();

        List<Image> images =
                IntStream.range(0, request.payloads().size())
                        .mapToObj(
                                i -> {
                                    AlbumImageUploadRequest.Payload req = request.payloads().get(i);
                                    String presignedUrl = presignedUrls.get(i);

                                    String objectUrl =
                                            presignedUrl.substring(0, presignedUrl.indexOf("?"));

                                    return Image.createImage(
                                            album,
                                            currentMember.getId(),
                                            objectUrl,
                                            req.generatedAt() != null
                                                    ? req.generatedAt()
                                                    : LocalDateTime.now(),
                                            req.capacity());
                                })
                        .toList();

        imageRepository.bulkInsertImages(images);

        List<Long> imageIds =
                imageRepository.findImageIdsByUrlsInOrder(
                        images.stream().map(Image::getUrl).toList());

        List<ImageUploadListResponse.Payload> payloads =
                IntStream.range(0, images.size())
                        .mapToObj(
                                i ->
                                        ImageUploadListResponse.Payload.of(
                                                imageIds.get(i), presignedUrls.get(i)))
                        .toList();

        return ImageUploadListResponse.of(payloads, currentMember.getLocalImageDeletion());
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId, Long lastImageId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        Slice<AlbumImageListResponse> result =
                imageRepository.findAllByAlbumId(albumId, lastImageId, size, direction);
        return SliceResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<EventImageListResponse> getEventImages(
            Long eventId, Long lastImageId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);
        getParticipantByMemberIdAndAlbumId(currentMember.getId(), event.getAlbum().getId());

        Slice<EventImageListResponse> result =
                imageRepository.findAllByEventId(eventId, lastImageId, size, direction);
        return SliceResponse.from(result);
    }

    @Override
    public void deleteAlbumImage(Long albumId, AlbumImageDeleteRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        validateParticipantAuthority(currentMember.getId(), album.getId());

        List<Long> distinctImageIds =
                request.imageIds().stream().filter(Objects::nonNull).distinct().toList();
        List<Image> images = imageRepository.findAllById(distinctImageIds);

        validateImagesInAlbum(images, album);

        album.decreaseCapacity(
                images.stream()
                        .map(Image::getCapacityGb)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        eventPublisher.publishEvent(
                ImagesDeleteEvent.of(images.stream().map(Image::getUrl).toList()));
        imageRepository.deleteAllInBatch(images);
    }

    @Override
    @Transactional
    public TempAlbumImageUploadListResponse createTempAlbumImageUploadUrls(
            Long tempAlbumId, TempAlbumImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        validateTempAlbumOwner(tempAlbum, currentMember);

        BigDecimal uploadCapacity =
                request.payloads().stream()
                        .map(TempAlbumImageUploadRequest.Payload::capacity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateTempAlbumCapacity(tempAlbum, uploadCapacity);
        validateDistinctHashes(request);

        tempAlbum.increaseCapacity(uploadCapacity);

        List<String> presignedUrls =
                request.payloads().stream()
                        .map(
                                req ->
                                        s3Util.createPresignedUrl(
                                                ImageType.TEMP_ALBUM_IMAGE,
                                                tempAlbum.getId(),
                                                req.fileExtension(),
                                                req.md5Hashes()))
                        .toList();

        List<TempAlbumImage> tempAlbumImages =
                IntStream.range(0, request.payloads().size())
                        .mapToObj(
                                i -> {
                                    TempAlbumImageUploadRequest.Payload req =
                                            request.payloads().get(i);
                                    String presignedUrl = presignedUrls.get(i);

                                    String objectUrl =
                                            presignedUrl.substring(0, presignedUrl.indexOf("?"));

                                    return TempAlbumImage.createTempAlbumImage(
                                            tempAlbum, objectUrl, req.capacity());
                                })
                        .toList();

        imageRepository.bulkInsertTempAlbumImages(tempAlbumImages);

        List<Long> tempAlbumImageIds =
                imageRepository.findTempImageIdsByUrlsInOrder(
                        tempAlbumImages.stream().map(TempAlbumImage::getUrl).toList());

        List<TempAlbumImageUploadListResponse.Payload> payloads =
                IntStream.range(0, tempAlbumImageIds.size())
                        .mapToObj(
                                i ->
                                        TempAlbumImageUploadListResponse.Payload.of(
                                                tempAlbumImageIds.get(i), presignedUrls.get(i)))
                        .toList();

        return TempAlbumImageUploadListResponse.of(payloads);
    }

    @Override
    @Transactional
    public void deleteTempAlbumImage(Long tempAlbumId, TempAlbumImageDeleteRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        validateTempAlbumOwner(tempAlbum, currentMember);

        List<Long> distinctTempAlbumImageIds =
                request.tempAlbumImageIds().stream().filter(Objects::nonNull).distinct().toList();

        List<TempAlbumImage> tempAlbumImages =
                tempAlbumImageRepository.findAllById(distinctTempAlbumImageIds);

        validateTempAlbumImagesInTempAlbum(tempAlbumImages, tempAlbum);

        tempAlbum.decreaseCapacity(
                tempAlbumImages.stream()
                        .map(TempAlbumImage::getCapacityGb)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        eventPublisher.publishEvent(
                TempAlbumImagesDeleteEvent.of(
                        tempAlbumImages.stream().map(TempAlbumImage::getUrl).toList()));
        tempAlbumImageRepository.deleteAllInBatch(tempAlbumImages);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Album getAlbumByIdWithLock(Long albumId) {
        return albumRepository
                .findByIdWithPessimisticLock(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Event getEventById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new CustomException(EventErrorCode.EVENT_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private TempAlbum getTempAlbumById(Long tempAlbumId) {
        return tempAlbumRepository
                .findById(tempAlbumId)
                .orElseThrow(() -> new CustomException(TempAlbumErrorCode.TEMP_ALBUM_NOT_FOUND));
    }

    private void validateParticipantAuthority(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (participant.getRole().equals(ParticipantRole.LIMITED)) {
            throw new CustomException(AlbumErrorCode.LIMITED_AUTHORITY);
        }
    }

    private void validateTempAlbumOwner(TempAlbum tempAlbum, Member member) {
        if (!Objects.equals(tempAlbum.getMember().getId(), member.getId())) {
            throw new CustomException(TempAlbumErrorCode.NOT_TEMP_ALBUM_OWNER);
        }
    }

    private void validateAlbumCapacity(Album album, BigDecimal uploadCapacity) {

        BigDecimal maxCapacity = album.getType().getCapacityGb();
        BigDecimal current = album.getCapacityGb();
        BigDecimal afterUpload = current.add(uploadCapacity);

        if (afterUpload.compareTo(maxCapacity) > 0) {
            throw new CustomException(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED);
        }
    }

    private void validateTempAlbumCapacity(TempAlbum tempAlbum, BigDecimal uploadCapacity) {

        BigDecimal maxCapacity = tempAlbum.getType().getCapacityGb();
        BigDecimal current = tempAlbum.getCapacityGb();
        BigDecimal afterUpload = current.add(uploadCapacity);

        if (afterUpload.compareTo(maxCapacity) > 0) {
            throw new CustomException(TempAlbumErrorCode.TEMP_ALBUM_CAPACITY_EXCEEDED);
        }
    }

    private void validateDistinctHashes(AlbumImageUploadRequest request) {
        List<String> hashes =
                request.payloads().stream()
                        .map(AlbumImageUploadRequest.Payload::md5Hashes)
                        .toList();

        if (hashes.stream().distinct().count() != hashes.size()) {
            throw new CustomException(ImageErrorCode.DUPLICATE_HASHES);
        }
    }

    private void validateDistinctHashes(TempAlbumImageUploadRequest request) {
        List<String> hashes =
                request.payloads().stream()
                        .map(TempAlbumImageUploadRequest.Payload::md5Hashes)
                        .toList();

        if (hashes.stream().distinct().count() != hashes.size()) {
            throw new CustomException(ImageErrorCode.DUPLICATE_HASHES);
        }
    }

    private void validateImageExtension(FileExtension extension) {
        if (!FileExtension.getImageExtensions().contains(extension)) {
            throw new CustomException(ImageErrorCode.NOT_IMAGE_EXTENSION);
        }
    }

    private void validateImagesInAlbum(List<Image> images, Album album) {
        boolean containsNotInAlbum =
                images.stream().anyMatch(ei -> !ei.getAlbum().getId().equals(album.getId()));

        if (containsNotInAlbum) {
            throw new CustomException(AlbumErrorCode.IMAGES_NOT_IN_ALBUM);
        }
    }

    private void validateTempAlbumImagesInTempAlbum(
            List<TempAlbumImage> tempAlbumImages, TempAlbum tempAlbum) {
        boolean containsNotInTempAlbum =
                tempAlbumImages.stream()
                        .anyMatch(ei -> !ei.getTempAlbum().getId().equals(tempAlbum.getId()));

        if (containsNotInTempAlbum) {
            throw new CustomException(TempAlbumErrorCode.IMAGES_NOT_IN_TEMP_ALBUM);
        }
    }

    private void validateSubscriptionNotExpired(Album album) {
        if (album.getType() == AlbumType.BASIC) return;

        if (album.getSubscription().getStatus() == SubscriptionStatus.EXPIRED) {
            throw new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION);
        }
    }
}
