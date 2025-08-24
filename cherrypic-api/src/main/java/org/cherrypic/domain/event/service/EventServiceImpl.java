package org.cherrypic.domain.event.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventImageAddRequest;
import org.cherrypic.domain.event.dto.request.EventImageRemoveRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventCreateResponse;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.dto.response.EventUpdateResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.EventImageRepository;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.entity.EventImage;
import org.cherrypic.exception.CustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.global.util.S3Util;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Slice;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final MemberUtil memberUtil;
    private final S3Util s3Util;

    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final ImageRepository imageRepository;
    private final EventImageRepository eventImageRepository;

    @Override
    public EventCreateResponse createEvent(EventCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(request.albumId());

        validateParticipantAuthority(currentMember, album);

        Event event = Event.createEvent(album, request.title(), request.coverUrl());
        eventRepository.save(event);

        return EventCreateResponse.from(event);
    }

    @Override
    public EventUpdateResponse updateEvent(Long eventId, EventUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);

        validateParticipantAuthority(currentMember, event.getAlbum());

        if (event.getCoverUrl() != null && !event.getCoverUrl().equals(request.coverUrl())) {
            s3Util.deleteFileFromS3(event.getCoverUrl());
        }

        event.updateEvent(request.title(), request.coverUrl());

        return EventUpdateResponse.from(event);
    }

    @Override
    @Transactional(readOnly = true)
    public SliceResponse<EventListResponse> getAlbumEvents(
            Long albumId, Long lastEventId, int size, SortDirection direction) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());

        Slice<EventListResponse> result =
                eventRepository.findAllByAlbumId(album.getId(), lastEventId, size, direction);

        return SliceResponse.from(result);
    }

    @Override
    public void deleteEvent(Long eventId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);

        validateParticipantAuthority(currentMember, event.getAlbum());

        eventRepository.delete(event);
    }

    @Override
    @Retryable(
            retryFor = {DataIntegrityViolationException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 0))
    public void addImages(Long eventId, EventImageAddRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);

        List<Long> distinctImageIds =
                request.imageIds().stream().filter(Objects::nonNull).distinct().toList();

        validateParticipantAuthority(currentMember, event.getAlbum());
        validateAllImageExistence(distinctImageIds);
        validateAllImageAlbum(distinctImageIds, eventId);

        List<Image> images = getAllUnmappedImagesById(eventId, distinctImageIds);
        if (images.isEmpty()) return; // 사용자가 모두 해당 event에 이미 속하는 사진만 고른 경우

        List<EventImage> eventImages =
                images.stream().map(image -> EventImage.createEventImage(event, image)).toList();

        try {
            eventImageRepository.saveAllAndFlush(eventImages);
        } catch (DataIntegrityViolationException e) {
            String constraint = getMySqlConstraint(e);
            if ("fk_event_image_event".equalsIgnoreCase(constraint)) {
                throw new CustomException(EventErrorCode.EVENT_DELETED);
            }
            if ("fk_event_image_image".equalsIgnoreCase(constraint)) {
                throw new CustomException(ImageErrorCode.IMAGE_DELETED);
            }
            if ("uk_event_image_event_id_image_id".equalsIgnoreCase(constraint)) {
                throw e;
            }
            throw new CustomException(ImageErrorCode.IMAGE_CONFLICT);
        }
    }

    @Override
    public void removeImages(Long eventId, EventImageRemoveRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);

        validateParticipantAuthority(currentMember, event.getAlbum());

        List<Long> distinctEventImagesIds =
                request.eventImageIds().stream().filter(Objects::nonNull).distinct().toList();

        List<EventImage> eventImages = eventImageRepository.findAllById(distinctEventImagesIds);
        if (eventImages.isEmpty()) return; // 이미 모두 삭제된 경우

        validateEventImagesInEvent(eventImages, event);

        eventImageRepository.deleteAllInBatch(eventImages);
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

    private void validateParticipantAuthority(Member member, Album album) {

        Participant participant = getParticipantByMemberIdAndAlbumId(member.getId(), album.getId());

        boolean isLimited = participant.getRole().equals(ParticipantRole.LIMITED);
        if (isLimited) {
            throw new CustomException(AlbumErrorCode.LIMITED_AUTHORITY);
        }
    }

    private void validateAllImageAlbum(List<Long> imageIds, Long albumId) {
        if (imageRepository.countByIdInAndAlbumId(imageIds, albumId) != imageIds.size()) {
            throw new CustomException(ImageErrorCode.IMAGES_IN_OTHER_ALBUM);
        }
    }

    private void validateAllImageExistence(List<Long> imageIds) {
        if (imageRepository.countByIdIn(imageIds) != imageIds.size()) {
            throw new CustomException(ImageErrorCode.IMAGES_NOT_FOUND);
        }
    }

    private void validateEventImagesInEvent(List<EventImage> eventImages, Event event) {
        boolean containsNotInEvent =
                eventImages.stream().anyMatch(ei -> !ei.getEvent().getId().equals(event.getId()));

        if (containsNotInEvent) {
            throw new CustomException(EventErrorCode.EVENT_IMAGES_NOT_IN_EVENT);
        }
    }

    private List<Image> getAllUnmappedImagesById(Long eventId, List<Long> imageIds) {
        return imageRepository.findAllUnmappedToEvent(eventId, imageIds);
    }

    private String getMySqlConstraint(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof java.sql.SQLIntegrityConstraintViolationException sql) {
                String msg = sql.getMessage();
                if (msg != null) {
                    String lower = msg.toLowerCase();
                    if (lower.contains("fk_event_image_event")) return "fk_event_image_event";
                    if (lower.contains("fk_event_image_image")) return "fk_event_image_image";
                    if (lower.contains("uk_event_image_event_id_image_id"))
                        return "uk_event_image_event_id_image_id";
                }
                return String.valueOf(sql.getErrorCode()); // fallback
            }
            t = t.getCause();
        }
        return null;
    }
}
