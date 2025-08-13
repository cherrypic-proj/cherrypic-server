package org.cherrypic.domain.participant.service;

public interface ParticipantService {
    void leaveAlbum(Long albumId);

    void kickParticipant(Long albumId, Long participantId);
}
