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
import org.cherrypic.domain.event.repository.EventImageRepository;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.event.ImagesDeleteEvent;
import org.cherrypic.domain.image.dto.request.*;
import org.cherrypic.domain.image.dto.response.*;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.domain.subscription.exception.SubscriptionErrorCode;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.cherrypic.domain.tempalbum.dto.event.TempAlbumImagesDeleteEvent;
import org.cherrypic.domain.tempalbum.exception.TempAlbumErrorCode;
import org.cherrypic.domain.tempalbum.repository.TempAlbumImageRepository;
import org.cherrypic.domain.tempalbum.repository.TempAlbumRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.s3.S3Util;
import org.cherrypic.s3.enums.FileExtension;
import org.cherrypic.s3.enums.ImageType;
import org.cherrypic.subscription.entity.Subscription;
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
    private final SubscriptionRepository subscriptionRepository;
    private final TempAlbumRepository tempAlbumRepository;
    private final TempAlbumImageRepository tempAlbumImageRepository;
    private final EventImageRepository eventImageRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ImagePresignedUrlResponse createMemberProfileImageUploadUrl(
            ImageUploadUrlRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.MEMBER_PROFILE,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return ImagePresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public ImagePresignedUrlResponse createAlbumCoverImageUploadUrl(ImageUploadUrlRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.ALBUM_COVER,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return ImagePresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public ImagePresignedUrlResponse createEventCoverImageUploadUrl(ImageUploadUrlRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        validateImageExtension(request.fileExtension());

        String presignedUrl =
                s3Util.createPresignedUrl(
                        ImageType.EVENT_COVER,
                        currentMember.getId(),
                        request.fileExtension(),
                        request.md5Hash());

        return ImagePresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public AlbumImagesPresignedUrlResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImagesUploadUrlRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumByIdWithLock(albumId);

        validateParticipantAuthority(currentMember.getId(), album.getId());
        validateSubscriptionNotExpired(album);

        BigDecimal uploadCapacityMb =
                request.payloads().stream()
                        .map(AlbumImagesUploadUrlRequest.Payload::capacityMb)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateAlbumCapacity(album, uploadCapacityMb);
        validateDistinctHashes(request);

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

        return AlbumImagesPresignedUrlResponse.of(presignedUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        Slice<AlbumImageListResponse> result =
                imageRepository.findAllByAlbumId(albumId, lastImageId, size, parameter, direction);
        return SliceResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<EventImageListResponse> getEventImages(
            Long eventId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);
        getParticipantByMemberIdAndAlbumId(currentMember.getId(), event.getAlbum().getId());

        Slice<EventImageListResponse> result =
                imageRepository.findAllByEventId(eventId, lastImageId, size, parameter, direction);
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
                        .map(Image::getCapacityMb)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        eventPublisher.publishEvent(
                ImagesDeleteEvent.of(images.stream().map(Image::getUrl).toList()));

        eventImageRepository.deleteAllByImages(images);
        imageRepository.deleteAllInBatch(images);
    }

    @Override
    @Transactional
    public TempAlbumImagesPresignedUrlResponse createTempAlbumImageUploadUrls(
            Long tempAlbumId, TempAlbumImagesUploadUrlRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        validateTempAlbumOwner(tempAlbum, currentMember);

        BigDecimal uploadCapacityMb =
                request.payloads().stream()
                        .map(TempAlbumImagesUploadUrlRequest.Payload::capacityMb)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateTempAlbumCapacity(tempAlbum, uploadCapacityMb);
        validateDistinctHashes(request);

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

        return TempAlbumImagesPresignedUrlResponse.of(presignedUrls);
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
                        .map(TempAlbumImage::getCapacityMb)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        eventPublisher.publishEvent(
                TempAlbumImagesDeleteEvent.of(
                        tempAlbumImages.stream().map(TempAlbumImage::getUrl).toList()));
        tempAlbumImageRepository.deleteAllInBatch(tempAlbumImages);
    }

    @Override
    public void completeNonAlbumImageUpload(ImageUploadCompleteRequest request) {
        validateImageUpload(request.imageUrl());

        s3Util.updateTagToCompleteByUrl(request.imageUrl());
    }

    @Override
    public AlbumImagesUploadCompleteResponse completeAlbumImagesUpload(
            Long albumId, AlbumImagesUploadCompleteRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);

        List<String> imageUrls =
                request.payloads().stream()
                        .map(AlbumImagesUploadCompleteRequest.Payload::imageUrl)
                        .toList();

        validateImagesUpload(imageUrls);

        s3Util.updateTagsToCompleteByUrls(imageUrls);

        BigDecimal uploadCapacityMb =
                request.payloads().stream()
                        .map(AlbumImagesUploadCompleteRequest.Payload::capacityMb)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        album.increaseCapacity(uploadCapacityMb);

        List<Image> images =
                IntStream.range(0, request.payloads().size())
                        .mapToObj(
                                i -> {
                                    AlbumImagesUploadCompleteRequest.Payload req =
                                            request.payloads().get(i);
                                    String imageUrl = imageUrls.get(i);

                                    return Image.createImage(
                                            album,
                                            currentMember.getId(),
                                            imageUrl,
                                            req.generatedAt() != null
                                                    ? req.generatedAt()
                                                    : LocalDateTime.now(),
                                            req.capacityMb());
                                })
                        .toList();

        imageRepository.bulkInsertImages(images);

        List<Long> imageIds =
                imageRepository.findImageIdsByUrlsInOrder(
                        images.stream().map(Image::getUrl).toList());

        return AlbumImagesUploadCompleteResponse.of(imageIds);
    }

    @Override
    public TempAlbumImagesUploadCompleteResponse completeTempAlbumImagesUpload(
            Long tempAlbumId, TempAlbumImagesUploadCompleteRequest request) {
        final TempAlbum tempAlbum = getTempAlbumById(tempAlbumId);

        List<String> imageUrls =
                request.payloads().stream()
                        .map(TempAlbumImagesUploadCompleteRequest.Payload::tempAlbumImageUrl)
                        .toList();

        validateImagesUpload(imageUrls);

        s3Util.updateTagsToCompleteByUrls(imageUrls);

        BigDecimal uploadCapacityMb =
                request.payloads().stream()
                        .map(TempAlbumImagesUploadCompleteRequest.Payload::capacityMb)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        tempAlbum.increaseCapacity(uploadCapacityMb);

        List<TempAlbumImage> tempAlbumImages =
                IntStream.range(0, request.payloads().size())
                        .mapToObj(
                                i -> {
                                    TempAlbumImagesUploadCompleteRequest.Payload req =
                                            request.payloads().get(i);
                                    String imageUrl = imageUrls.get(i);

                                    return TempAlbumImage.createTempAlbumImage(
                                            tempAlbum, imageUrl, req.capacityMb());
                                })
                        .toList();

        imageRepository.bulkInsertTempAlbumImages(tempAlbumImages);

        List<Long> tempAlbumImageIds =
                imageRepository.findTempImageIdsByUrlsInOrder(
                        tempAlbumImages.stream().map(TempAlbumImage::getUrl).toList());

        return TempAlbumImagesUploadCompleteResponse.of(tempAlbumImageIds);
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

    private Subscription getSubscriptionByAlbumId(Long albumId) {
        return subscriptionRepository
                .findByAlbumId(albumId)
                .orElseThrow(
                        () -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
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
        BigDecimal maxCapacity = album.getType().getCapacityMb();
        BigDecimal current = album.getCapacityMb();
        BigDecimal afterUpload = current.add(uploadCapacity);

        if (afterUpload.compareTo(maxCapacity) > 0) {
            throw new CustomException(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED);
        }
    }

    private void validateTempAlbumCapacity(TempAlbum tempAlbum, BigDecimal uploadCapacity) {
        BigDecimal maxCapacity = tempAlbum.getType().getCapacityMb();
        BigDecimal current = tempAlbum.getCapacityMb();
        BigDecimal afterUpload = current.add(uploadCapacity);

        if (afterUpload.compareTo(maxCapacity) > 0) {
            throw new CustomException(TempAlbumErrorCode.TEMP_ALBUM_CAPACITY_EXCEEDED);
        }
    }

    private void validateDistinctHashes(AlbumImagesUploadUrlRequest request) {
        List<String> hashes =
                request.payloads().stream()
                        .map(AlbumImagesUploadUrlRequest.Payload::md5Hashes)
                        .toList();

        if (hashes.stream().distinct().count() != hashes.size()) {
            throw new CustomException(ImageErrorCode.DUPLICATE_HASHES);
        }
    }

    private void validateDistinctHashes(TempAlbumImagesUploadUrlRequest request) {
        List<String> hashes =
                request.payloads().stream()
                        .map(TempAlbumImagesUploadUrlRequest.Payload::md5Hashes)
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

        Subscription subscription = getSubscriptionByAlbumId(album.getId());
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new CustomException(AlbumErrorCode.EXPIRED_SUBSCRIPTION);
        }
    }

    private void validateImageUpload(String imageUrl) {
        if (!s3Util.doesFileExistByUrl(imageUrl)) {
            throw new CustomException(ImageErrorCode.IMAGE_UPLOAD_FAIL);
        }
    }

    private void validateImagesUpload(List<String> imageUrls) {
        if (!s3Util.doAllFilesExistByUrls(imageUrls)) {
            throw new CustomException(ImageErrorCode.IMAGE_UPLOAD_FAIL);
        }
    }
}
