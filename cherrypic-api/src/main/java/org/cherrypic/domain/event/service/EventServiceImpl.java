package org.cherrypic.domain.event.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.exception.AlbumException;
import org.cherrypic.domain.event.dto.EventCreateRequest;
import org.cherrypic.domain.event.dto.EventCreateResponse;
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
        final Album currentAlbum = getAlbumById(request.albumId());
        validateAlbumParticipant(currentMember, currentAlbum);

        Event event = Event.createEvent(currentAlbum, request.title(), request.coverUrl());
        eventRepository.save(event);

        return EventCreateResponse.from(event, currentAlbum);
    }

    private Album getAlbumById(Long id) {
        return albumRepository
                .findById(id)
                .orElseThrow(() -> new AlbumException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private void validateAlbumParticipant(Member participant, Album currentAlbum) {
        if (!participantRepository.existsByMemberIdAndAlbumId(
                participant.getId(), currentAlbum.getId())) {
            throw new EventException(EventErrorCode.NOT_ALBUM_PARTICIPANT);
        }
    }
}
