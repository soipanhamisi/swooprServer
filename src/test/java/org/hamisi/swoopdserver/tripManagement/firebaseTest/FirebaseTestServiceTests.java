package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.hamisi.swoopdserver.notificationUtilities.FirebaseProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FirebaseTestServiceTests {

    @Mock
    private FirebaseProxy firebaseProxy;

    @InjectMocks
    private FirebaseTestService firebaseTestService;

    @Test
    @DisplayName("Send notification delegates to FirebaseProxy")
    void sendNotificationDelegatesToFirebaseProxy() {
        firebaseTestService.sendNotification("  sample-token  ", "  Test message  ");

        verify(firebaseProxy, times(1)).sendNotification("sample-token", "Test message");
    }

    @Test
    @DisplayName("Send notification rejects blank Firebase token")
    void sendNotificationRejectsBlankFirebaseToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> firebaseTestService.sendNotification("   ", "Test message")
        );
    }

    @Test
    @DisplayName("Send notification rejects blank message")
    void sendNotificationRejectsBlankMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> firebaseTestService.sendNotification("sample-token", " ")
        );
    }
}

