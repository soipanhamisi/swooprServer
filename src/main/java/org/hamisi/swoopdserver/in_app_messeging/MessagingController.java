package org.hamisi.swoopdserver.in_app_messeging;

import org.hamisi.swoopdserver.common.ApiResponse;
import org.hamisi.swoopdserver.common.TokenManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/messaging")
public class MessagingController {

    private final TokenManagementService tokenManagementService;
    private final InAppTripMessagingService inAppTripMessagingService;

    public MessagingController(TokenManagementService tokenManagementService, InAppTripMessagingService inAppTripMessagingService) {
        this.tokenManagementService = tokenManagementService;
        this.inAppTripMessagingService = inAppTripMessagingService;
    }

    @PostMapping("/postMessage")
    public ResponseEntity<ApiResponse<Void>> postMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody String message
    ){
        UUID userId = tokenManagementService.verifyToken(authHeader).getUserId();
        inAppTripMessagingService.broadcastMessage(userId, message);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("message posted"));
    }

    @PostMapping("/broadcastMessage")
    public ResponseEntity<ApiResponse<Void>> broadCastMessage(@RequestBody String message){
        inAppTripMessagingService.broadcastMessageTest(message);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("message broadcasted"));
    }

}
