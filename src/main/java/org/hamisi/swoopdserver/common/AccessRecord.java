package org.hamisi.swoopdserver.common;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AccessRecord {
    private UUID userId;
    private String email;

    public AccessRecord(String userId, String email) {
        this.userId = UUID.fromString(userId);
        this.email = email;
    }
}
