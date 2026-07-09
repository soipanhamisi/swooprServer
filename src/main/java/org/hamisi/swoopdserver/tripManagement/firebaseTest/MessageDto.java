package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MessageDto {
    private String firebaseMessagingToken;
    private String message;
}
