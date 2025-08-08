package org.cherrypic.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    ALBUM("앨범"),
    ;

    private final String value;
}
