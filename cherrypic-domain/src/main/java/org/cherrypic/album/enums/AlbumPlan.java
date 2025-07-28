package org.cherrypic.album.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlbumPlan {
    BASIC(0),
    PRO(3900),
    PREMIUM(7900),
    ;

    private final int price;

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
