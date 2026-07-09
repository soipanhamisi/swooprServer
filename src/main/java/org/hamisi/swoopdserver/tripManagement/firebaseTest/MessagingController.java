package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sendMessage")
public class MessagingController {
    private final FirebaseMessagingService firebaseMessagingService;

    public MessagingController(FirebaseMessagingService firebaseMessagingService) {
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @PostMapping("firebase")
    public ResponseEntity<String> sendMessage(@RequestBody MessageDto messageDto){
        firebaseMessagingService.sendNotification(messageDto.getUserId(), messageDto.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body("Message Sent...");
    }
}
