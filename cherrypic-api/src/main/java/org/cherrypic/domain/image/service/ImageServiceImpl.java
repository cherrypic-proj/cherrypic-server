package org.cherrypic.domain.image.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
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

        return createPresignedUrl(
                ImageType.MEMBER_PROFILE, currentMember.getId(), request.imageFileExtension());
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId, Long lastImageId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();

        getAlbumById(albumId);
        getParticipantByMemberIdAndAlbumId(currentMember.getId(), albumId);

        Slice<AlbumImageListResponse> result =
                imageRepository.findAllByAlbumId(albumId, lastImageId, size, direction);
        return SliceResponse.from(result);
    }

    @Override
    public SliceResponse<EventImageListResponse> getEventImages(
            Long eventId, Long lastImageId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);
        getParticipantByMemberIdAndAlbumId(currentMember.getId(), event.getAlbum().getId());

        Slice<EventImageListResponse> result =
                imageRepository.findAllByEventId(eventId, lastImageId, size, direction);
        return SliceResponse.from(result);
    }

    private PresignedUrlResponse createPresignedUrl(
            ImageType imageType, Long targetId, ImageFileExtension imageFileExtension) {
        String imageKey = UUID.randomUUID().toString();
        String fileName = createFileName(imageType, targetId, imageKey, imageFileExtension);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generatePresignedUrlRequest(
                        s3Properties.bucket(), fileName, imageFileExtension.getExtension());

        String presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();

        return new PresignedUrlResponse(presignedUrl);
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

    private void validateAlbumEvent(Album album, Event event) {
        if (!event.getAlbum().getId().equals(album.getId())) {
            throw new CustomException(EventErrorCode.EVENT_DOESNT_BELONG_TO_ALBUM);
        }
    }
}
