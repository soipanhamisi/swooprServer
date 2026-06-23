package org.hamisi.swoopdserver.tripManagement.records;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.User;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserDestinationZone {
    private User user;
    private String destinationZone;
    private LocalDateTime preferredDepartureTime;
}
