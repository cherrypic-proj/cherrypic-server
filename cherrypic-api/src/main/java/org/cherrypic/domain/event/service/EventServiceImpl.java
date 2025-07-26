package org.cherrypic.domain.event.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
import org.cherrypic.domain.event.dto.EventUpdateRequest;
import org.cherrypic.domain.event.dto.EventUpdateResponse;
import org.cherrypic.domain.event.exception.EventErrorCode;
import org.cherrypic.domain.event.exception.EventException;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.repository.EventRepository;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.repository.ParticipantRepository;
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

    @Override
    public EventCreateResponse createEvent(EventCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();
        Album album = getAlbumById(request.albumId());
        validateAlbumParticipant(currentMember, album);

        Event event = Event.createEvent(album, request.title(), request.coverUrl());
        eventRepository.save(event);

        return EventCreateResponse.from(event);
    }

    @Override
    public EventUpdateResponse updateEvent(Long eventId, EventUpdateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        event.changeTitle(request.title());
        event.changeCoverUrl(request.coverUrl());

        return EventUpdateResponse.from(event);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private void validateAlbumParticipant(Member member, Album album) {
        if (!participantRepository.existsByMemberIdAndAlbumId(member.getId(), album.getId())) {
            throw new EventException(EventErrorCode.NOT_ALBUM_PARTICIPANT);
        }
    }
}
