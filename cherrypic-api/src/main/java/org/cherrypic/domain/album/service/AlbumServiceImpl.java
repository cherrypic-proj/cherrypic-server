package org.cherrypic.domain.album.service;

import lombok.RequiredArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.repository.AlbumRepository;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.entity.Participant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumServiceImpl implements AlbumService {

    private final MemberUtil memberUtil;
    private final AlbumRepository albumRepository;

    @Override
    public AlbumCreateResponse createAlbum(AlbumCreateRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        Album album = Album.createAlbum(request.title(), request.coverUrl(), request.type());
        Participant participant = Participant.createParticipant(currentMember, album);
        album.addParticipant(participant);

        albumRepository.save(album);

        return AlbumCreateResponse.from(album);
    }
}
