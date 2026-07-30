package org.hamisi.swoopdserver.notificationUtilities;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class NotificationConfig {
    /**
     * Provides a Jackson ObjectMapper bean for JSON serialization/deserialization
     * across the notification utilities package.
     * @return Configured ObjectMapper instance     */

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}
