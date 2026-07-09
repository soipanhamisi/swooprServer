package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sendMessage")
public class MessagingController {
    private final FirebaseTestService firebaseTestService;

    public MessagingController(FirebaseTestService firebaseTestService) {
        this.firebaseTestService = firebaseTestService;
    }

    @PostMapping("firebase")
    public ResponseEntity<String> sendMessage(@RequestBody MessageDto messageDto){
        firebaseTestService.sendNotification(messageDto.getFirebaseMessagingToken(), messageDto.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body("Message Sent...");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidFirebaseTestRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
