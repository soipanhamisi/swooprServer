package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tripId;
    @ManyToMany
    @JoinTable(
            name = "trip_users",
            joinColumns = @JoinColumn(name = "tripId"),
            inverseJoinColumns = @JoinColumn(name = "userId")
    )
    private List<User> users;
    @OneToOne
    @JoinColumn(name = "vehicleId")
    private Vehicle vehicle;
    @Column
    private int tripCapacity;
    @Column
    private TripStatus tripStatus;
    @Embedded
    private OriginDestination originDestination;
    @Column
    private String routePolyline;
    @Column
    private LocalDateTime departureTime;
    @Column
    private UUID createdBy;
}
