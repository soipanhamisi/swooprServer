package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hamisi.swoopdserver.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="tripMembership")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TripMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID )
    private UUID id;
    @ManyToOne
    private Trip trip;
    @ManyToOne
    private User user;
    @Embedded
    private OriginDestination coordinatePair;
    @Column
    private LocalDateTime preferredDepartureTime;



}
