package org.cherrypic.tempalbum.enums;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TempAlbumType {
    DEFAULT(new BigDecimal(1024), 3);

    private final BigDecimal capacityMb;
    private final int daysToLive;
}
