package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.User;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    @OneToOne
    @JoinColumn(name =  "userId")
    private User user;

    @Column
    private String vehicleRegNumber;
    @Column
    private String vehicleDescription;
}
