package org.hamisi.swoopdserver.tripManagement.firebaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessagingControllerTests {

    private FirebaseTestService firebaseTestService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        firebaseTestService = mock(FirebaseTestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MessagingController(firebaseTestService)).build();
    }

    @Test
    @DisplayName("POST /sendMessage/firebase sends notification without JWT header")
    void sendMessageReturnsOkWithoutJwtHeader() throws Exception {
        mockMvc.perform(post("/sendMessage/firebase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firebaseMessagingToken": "sample-token",
                                  "message": "Hello from Firebase test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Message Sent..."));

        verify(firebaseTestService, times(1))
                .sendNotification(eq("sample-token"), eq("Hello from Firebase test"));
    }

    @Test
    @DisplayName("POST /sendMessage/firebase returns bad request for invalid payload")
    void sendMessageReturnsBadRequestForInvalidPayload() throws Exception {
        doThrow(new IllegalArgumentException("Firebase messaging token is required"))
                .when(firebaseTestService)
                .sendNotification(eq(""), eq("Hello from Firebase test"));

        mockMvc.perform(post("/sendMessage/firebase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firebaseMessagingToken": "",
                                  "message": "Hello from Firebase test"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Firebase messaging token is required"));
    }
}


