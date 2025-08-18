package org.cherrypic.album.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlbumPlan {
    BASIC(0, 10),
    PRO(5900, 50),
    PREMIUM(12900, Integer.MAX_VALUE),
    ;

    private final int price;
    private final int maxParticipants;

    @JsonCreator
    public static AlbumPlan from(String plan) {
        return Stream.of(values())
                .filter(p -> p.name().equalsIgnoreCase(plan))
                .findFirst()
                .orElse(null);
    }

    public boolean requiresPayment() {
        return price > 0;
    }
}
