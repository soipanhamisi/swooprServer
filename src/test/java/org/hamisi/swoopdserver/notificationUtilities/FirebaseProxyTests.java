package org.hamisi.swoopdserver.notificationUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseProxyTests {

    @Mock
    private OAuthTokenProvider tokenProvider;

    @Mock
    private HttpResponse<String> httpResponse;

    private FirebaseProxy firebaseProxy;

    @BeforeEach
    void setUp() {
        firebaseProxy = new FirebaseProxy(tokenProvider);
        ReflectionTestUtils.setField(firebaseProxy, "projectId", "swoopd-test-project");
    }

    @Test
    @DisplayName("sendNotification throws when project id is missing")
    void sendNotificationThrowsWhenProjectIdMissing() {
        ReflectionTestUtils.setField(firebaseProxy, "projectId", "   ");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> firebaseProxy.sendNotification("{\"message\":{}}")
        );

        verify(tokenProvider, org.mockito.Mockito.never()).getAccessToken();
        assertEquals("GCP_PROJECT_ID is not configured", exception.getMessage());
    }

    @Test
    @DisplayName("buildFirebaseUrl uses the configured project id")
    void buildFirebaseUrlUsesConfiguredProjectId() {
        String firebaseUrl = ReflectionTestUtils.invokeMethod(firebaseProxy, "buildFirebaseUrl");

        assertEquals("https://fcm.googleapis.com/v1/projects/swoopd-test-project/messages:send", firebaseUrl);
    }

    @Test
    @DisplayName("getAccessToken delegates to OAuthTokenProvider")
    void getAccessTokenDelegatesToTokenProvider() {
        when(tokenProvider.getAccessToken()).thenReturn("access-token-123");
        String token = ReflectionTestUtils.invokeMethod(firebaseProxy, "getAccessToken");

        assertEquals("access-token-123", token);
        verify(tokenProvider).getAccessToken();
    }

    @Test
    @DisplayName("validateResponse throws when Firebase returns an error status")
    void validateResponseThrowsWhenStatusIsNotSuccess() {
        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn("permission denied");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(firebaseProxy, "validateResponse", httpResponse)
        );

        assertEquals("Firebase notification failed with status 403: permission denied", exception.getMessage());
    }
}


