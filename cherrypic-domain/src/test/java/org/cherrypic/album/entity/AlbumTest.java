package org.cherrypic.album.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.common.exception.DomainErrorCode;
import org.cherrypic.exception.CustomException;
import org.junit.jupiter.api.Test;

class AlbumTest {

    @Test
    void 용량을_4자리_GB_이상으로_늘릴_경우_예외가_발생한다() {
        // given
        Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumPlan.BASIC, false);

        // when & then
        assertThatThrownBy(() -> album.increaseCapacity(BigDecimal.valueOf(10000)))
                .isInstanceOf(CustomException.class)
                .hasMessage(DomainErrorCode.ALBUM_CAPACITY_INCREASE_OVER_LIMIT.getMessage());
    }

    @Test
    void 용량을_0GB_미만으로_줄일_경우_예외가_발생한다() {
        // given
        Album album = Album.createAlbum("testTitle", "testCoverUrl", AlbumPlan.BASIC, false);

        // when & then
        assertThatThrownBy(() -> album.decreaseCapacity(BigDecimal.valueOf(1)))
                .isInstanceOf(CustomException.class)
                .hasMessage(DomainErrorCode.ALBUM_CAPACITY_DECREASE_UNDER_ZERO.getMessage());
    }
}
