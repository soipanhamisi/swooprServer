package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.hamisi.swoopdserver.notificationUtilities.FirebaseProxy;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTestService {

    private final FirebaseProxy firebaseProxy;

    public FirebaseTestService(FirebaseProxy firebaseProxy) {
        this.firebaseProxy = firebaseProxy;
    }

    public void sendNotification(String firebaseMessagingToken, String message) {
        if (firebaseMessagingToken == null || firebaseMessagingToken.isBlank()) {
            throw new IllegalArgumentException("Firebase messaging token is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        firebaseProxy.sendNotification(firebaseMessagingToken.trim(), message.trim());
    }
}

