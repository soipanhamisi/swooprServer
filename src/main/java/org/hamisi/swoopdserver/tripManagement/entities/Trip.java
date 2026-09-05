package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.User;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
    @ManyToOne
    @JoinColumn(name = "vehicleId")
    private Vehicle vehicle;
    @Column
    private int tripCapacity;
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMembership> tripMembership = new ArrayList<TripMembership>();
    @Column
    @Enumerated(EnumType.STRING)
    private TripStatus tripStatus = TripStatus.OPEN;
    @Embedded
    private OriginDestination originDestination;
    @Column(columnDefinition = "LONGTEXT")
    private String routePolyline;
    @Column
    private LocalDateTime departureTime;
    @Column
    private UUID createdBy;
    @Column
    private String destinationZone;
    @Column
    private String originZone;
    @Column
    @Enumerated(EnumType.STRING)
    private TripDirection tripDirection;

    public void addUser(User userByUserId) {
        ensureUsersInitialized();
        if (userByUserId == null || containsUser(userByUserId)) {
            return;
        }
        this.users.add(userByUserId);
        this.tripCapacity--;
        if (this.tripCapacity == 0){
            this.tripStatus = TripStatus.FULL;
        }
    }


    private void ensureUsersInitialized() {
        if (this.users == null) {
            this.users = new ArrayList<>();
        }
    }

    private boolean containsUser(User candidate) {
        return this.users != null
                && this.users.stream().map(User::getUserId).filter(Objects::nonNull)
                .anyMatch(candidate.getUserId()::equals);
    }
}
