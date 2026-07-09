package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class MessageDto {
    private UUID userId;
    private String message;
}
