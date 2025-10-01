package org.cherrypic.album.entity.album;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.album.exception.AlbumDomainErrorCode;
import org.cherrypic.exception.CustomException;
import org.junit.jupiter.api.Test;

class AlbumTest {

    @Test
    void 용량을_7자리_MB_이상으로_늘릴_경우_예외가_발생한다() {
        // given
        Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumType.BASIC, false);

        // when & then
        assertThatThrownBy(() -> album.increaseCapacity(BigDecimal.valueOf(12345678)))
                .isInstanceOf(CustomException.class)
                .hasMessage(AlbumDomainErrorCode.ALBUM_CAPACITY_INCREASE_OVER_LIMIT.getMessage());
    }

    @Test
    void 용량을_0MB_미만으로_줄일_경우_예외가_발생한다() {
        // given
        Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumType.BASIC, false);

        // when & then
        assertThatThrownBy(() -> album.decreaseCapacity(BigDecimal.valueOf(1)))
                .isInstanceOf(CustomException.class)
                .hasMessage(AlbumDomainErrorCode.ALBUM_CAPACITY_DECREASE_UNDER_ZERO.getMessage());
    }
}
