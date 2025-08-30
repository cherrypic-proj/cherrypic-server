package org.cherrypic.album.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlbumType {
    BASIC(0, new BigDecimal("3"), 10),
    PRO(5900, new BigDecimal("200"), 50),
    PREMIUM(12900, new BigDecimal("2048"), Integer.MAX_VALUE);

    private final int price;
    private final BigDecimal capacityGb;
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
