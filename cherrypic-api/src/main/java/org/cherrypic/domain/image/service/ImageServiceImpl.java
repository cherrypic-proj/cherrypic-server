package org.cherrypic.domain.image.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.AlbumFileUploadRequest;
import org.cherrypic.domain.image.dto.request.AlbumImageDeleteRequest;
import org.cherrypic.domain.image.dto.request.ImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlsResponse;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.domain.image.enums.ImageType;
import org.cherrypic.domain.image.event.ImagesDeleteEvent;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.S3Util;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
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
    public PresignedUrlsResponse createAlbumFileUploadUrls(
            Long albumId, AlbumFileUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumByIdWithLock(albumId);

        BigDecimal uploadCapacity =
                request.payloads().stream()
                        .map(AlbumFileUploadRequest.Payload::capacity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateParticipantAuthority(currentMember.getId(), album.getId());
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
                                    AlbumFileUploadRequest.Payload req = request.payloads().get(i);
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

        return PresignedUrlsResponse.of(presignedUrls);
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

        eventPublisher.publishEvent(
                ImagesDeleteEvent.of(images.stream().map(Image::getUrl).toList()));
        imageRepository.deleteAllInBatch(images);
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

    private void validateParticipantAuthority(Long memberId, Long albumId) {
        Participant participant = getParticipantByMemberIdAndAlbumId(memberId, albumId);

        if (participant.getRole().equals(ParticipantRole.LIMITED)) {
            throw new CustomException(AlbumErrorCode.LIMITED_AUTHORITY);
        }
    }

    private void validatePresignedImageOwnership(Member member, List<Image> images) {
        Long memberId = member.getId();

        boolean hasInvalidImage =
                images.stream().anyMatch(image -> !image.getMemberId().equals(memberId));

        if (hasInvalidImage) {
            throw new CustomException(ImageErrorCode.PRESIGNED_IMAGES_NOT_MINE);
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

    private void validateDistinctHashes(AlbumFileUploadRequest request) {
        List<String> hashes =
                request.payloads().stream().map(AlbumFileUploadRequest.Payload::md5Hashes).toList();

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
}
