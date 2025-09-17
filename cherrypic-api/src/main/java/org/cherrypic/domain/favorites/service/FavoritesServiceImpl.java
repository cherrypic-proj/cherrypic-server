package org.cherrypic.domain.favorites.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.domain.album.exception.AlbumErrorCode;
import org.cherrypic.domain.album.repository.AlbumRepository;
import org.cherrypic.domain.favorites.dto.response.FavoritesMarkToggleResponse;
import org.cherrypic.domain.favorites.exception.FavoritesErrorCode;
import org.cherrypic.domain.favorites.repository.FavoritesRepository;
import org.cherrypic.domain.participant.repository.ParticipantRepository;
import org.cherrypic.exception.CustomException;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoritesServiceImpl implements FavoritesService {

    private final MemberUtil memberUtil;

    private final AlbumRepository albumRepository;
    private final ParticipantRepository participantRepository;
    private final FavoritesRepository favoritesRepository;

    @Override
    public FavoritesMarkToggleResponse toggleMark(Long albumId) {
        final Member currentMember = memberUtil.getCurrentMember();
        final Album album = getAlbumById(albumId);
        final Participant participant =
                getParticipantByMemberIdAndAlbumId(currentMember.getId(), album.getId());
        final Favorites favorites = getFavoritesByParticipantId(participant.getId());

        favorites.toggleMarked();

        return FavoritesMarkToggleResponse.from(favorites);
    }

    private Album getAlbumById(Long albumId) {
        return albumRepository
                .findById(albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.ALBUM_NOT_FOUND));
    }

    private Participant getParticipantByMemberIdAndAlbumId(Long memberId, Long albumId) {
        return participantRepository
                .findByMemberIdAndAlbumId(memberId, albumId)
                .orElseThrow(() -> new CustomException(AlbumErrorCode.NOT_ALBUM_PARTICIPANT));
    }

    private Favorites getFavoritesByParticipantId(Long participantId) {
        return favoritesRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new CustomException(FavoritesErrorCode.FAVORITES_NOT_FOUND));
    }
}
