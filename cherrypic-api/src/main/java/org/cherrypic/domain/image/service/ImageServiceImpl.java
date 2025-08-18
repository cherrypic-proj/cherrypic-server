package org.cherrypic.domain.image.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.AlbumImageUploadRequest;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlsResponse;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.domain.image.enums.ImageType;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.cherrypic.s3.S3Properties;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {

    private final MemberUtil memberUtil;
    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    private final AlbumRepository albumRepository;
    private final ImageRepository imageRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    @Override
    public PresignedUrlResponse createMemberProfileImageUploadUrl(
            MemberProfileImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        String presignedUrl =
                createPresignedUrl(
                        ImageType.MEMBER_PROFILE,
                        currentMember.getId(),
                        request.imageFileExtension());

        return PresignedUrlResponse.of(presignedUrl);
    }

    @Override
    public PresignedUrlsResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumByIdWithLock(albumId);

        validateParticipantAuthority(currentMember.getId(), album.getId());
        validateAlbumCapacity(album, request.capacity());

        album.increaseCapacity(request.capacity());

        List<String> presignedUrls =
                request.imageFileExtensions().stream()
                        .map(
                                extension -> {
                                    return createPresignedUrl(
                                            ImageType.ALBUM_IMAGE,
                                            currentMember.getId(),
                                            extension);
                                })
                        .toList();

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

    private String createPresignedUrl(
            ImageType imageType, Long targetId, ImageFileExtension imageFileExtension) {
        String imageKey = UUID.randomUUID().toString();
        String fileName = createFileName(imageType, targetId, imageKey, imageFileExtension);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generatePresignedUrlRequest(
                        s3Properties.bucket(), fileName, imageFileExtension.getExtension());

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    private String createFileName(
            ImageType imageType,
            Long targetId,
            String imageKey,
            ImageFileExtension imageFileExtension) {
        return springEnvironmentHelper.getCurrentProfile()
                + "/"
                + imageType.getType()
                + "/"
                + targetId
                + "/"
                + imageKey
                + "."
                + imageFileExtension.getExtension();
    }

    private GeneratePresignedUrlRequest generatePresignedUrlRequest(
            String bucket, String fileName, String imageFileExtension) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName, HttpMethod.PUT)
                        .withKey(fileName)
                        .withContentType("image/" + imageFileExtension)
                        .withExpiration(getPresignedUrlExpiration());

        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());

        return generatePresignedUrlRequest;
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += TimeUnit.MINUTES.toMillis(1);
        expiration.setTime(expTimeMillis);

        return expiration;
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

    private void validateAlbumCapacity(Album album, BigDecimal additionalUpload) {
        BigDecimal maxCapacity = album.getPlan().getCapacityGb();
        BigDecimal current = album.getCapacityGb();
        BigDecimal afterUpload = current.add(additionalUpload);

        if (afterUpload.compareTo(maxCapacity) > 0) {
            throw new CustomException(AlbumErrorCode.ALBUM_CAPACITY_EXCEEDED);
        }
    }
}
