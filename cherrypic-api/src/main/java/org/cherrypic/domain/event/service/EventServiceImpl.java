package org.cherrypic.domain.event.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.event.dto.request.EventCreateRequest;
import org.cherrypic.domain.event.dto.request.EventImageAddRequest;
import org.cherrypic.domain.event.dto.request.EventUpdateRequest;
import org.cherrypic.domain.event.dto.response.EventCreateResponse;
import org.cherrypic.domain.event.dto.response.EventListResponse;
import org.cherrypic.domain.event.dto.response.EventUpdateResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.domain.event.repository.EventRepository;
import org.cherrypic.domain.image.exception.ImageErrorCode;
import org.cherrypic.domain.image.repository.ImageRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.BaseCustomException;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.image.entity.Image;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.participant.enums.ParticipantRole;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final MemberUtil memberUtil;

    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final ImageRepository imageRepository;

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
    public void addImages(Long eventId, EventImageAddRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Event event = getEventById(eventId);
        final List<Image> images = getAllImagesById(request.imageIds());

        validateParticipantAuthority(currentMember, event.getAlbum());
        validateImageEvent(images);
        validateImageAlbum(images, event);

        List<String> keys =
                images.stream().map(img -> img.getId() + ":" + img.getVersion()).toList();

        int updated = imageRepository.bulkChangeImageEventWithVersionCheck(keys, eventId);
        if (updated != request.imageIds().size()) {
            throw new BaseCustomException(ImageErrorCode.SOME_IMAGES_HAS_CONFLICT);
        }
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Event getEventById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new EventException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private void validateParticipantAuthority(Member member, Album album) {

        Participant participant = getParticipantByMemberIdAndAlbumId(member.getId(), album.getId());

        boolean isLimited = participant.getRole().equals(ParticipantRole.LIMITED);
        if (isLimited) {
            throw new EventException(AlbumErrorCode.LIMITED_AUTHORITY);
        }
    }

    private void validateImageEvent(List<Image> images) {
        images.stream()
                .filter(image -> image.getEvent() != null)
                .findAny()
                .ifPresent(
                        img -> {
                            throw new BaseCustomException(ImageErrorCode.SOME_IMAGES_HAS_EVENT);
                        });
    }

    private void validateImageAlbum(List<Image> images, Event event) {
        images.stream()
                .filter(
                        image ->
                                !Objects.equals(image.getAlbum().getId(), event.getAlbum().getId()))
                .findAny()
                .ifPresent(
                        img -> {
                            throw new BaseCustomException(
                                    ImageErrorCode.SOME_IMAGES_NOT_FROM_CURRENT_ALBUM);
                        });
    }

    private List<Image> getAllImagesById(List<Long> imageIds) {
        return Optional.of(imageRepository.findAllById(imageIds))
                .filter(images -> images.size() == imageIds.size())
                .orElseThrow(
                        () -> new BaseCustomException(ImageErrorCode.SOME_IMAGES_ARE_NOT_FOUND));
    }
}
