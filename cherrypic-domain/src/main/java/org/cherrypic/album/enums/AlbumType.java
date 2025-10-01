package org.cherrypic.album.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** BASIC : 3GB PRO : 200GB PREMIUM : 2TB */
@Getter
@AllArgsConstructor
public enum AlbumType {
    BASIC(0, new BigDecimal("3072"), 10),
    PRO(5900, new BigDecimal("204800"), 50),
    PREMIUM(12900, new BigDecimal("2097152"), Integer.MAX_VALUE);

    private final int price;
    private final BigDecimal capacityMb;
    private final int maxParticipants;

    @JsonCreator
    public static AlbumType from(String type) {
        return Stream.of(values())
                .filter(p -> p.name().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    public boolean requiresPayment() {
        return price > 0;
    }
}
