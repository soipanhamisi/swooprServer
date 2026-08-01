package org.hamisi.swoopdserver.in_app_messeging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageDto {
    private LocalDateTime timeStamp;
    private String senderName;
    private String message;
}
