package org.cherrypic.tempalbum.enums;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TempAlbumType {
    DEFAULT(BigDecimal.ONE, 4);

    private final BigDecimal capacityGb;
    private final int daysToLive;
}
