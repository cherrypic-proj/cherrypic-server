package org.cherrypic.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StorageUnitConverter {

    private static final BigDecimal MB_IN_GB = BigDecimal.valueOf(1024);

    public static BigDecimal mbToGb(BigDecimal mb) {
        if (mb == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return mb.divide(MB_IN_GB, 2, RoundingMode.HALF_UP);
    }
}
