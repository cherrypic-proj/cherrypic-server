package org.cherrypic.participant.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;

public enum ParticipantRole {
    HOST,
    STANDARD,
    LIMITED,
    ;

    @JsonCreator
    public static ParticipantRole from(String role) {
        return Stream.of(values())
                .filter(p -> p.name().equalsIgnoreCase(role))
                .findFirst()
                .orElse(null);
    }
}
