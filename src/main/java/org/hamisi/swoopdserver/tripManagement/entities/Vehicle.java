package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hamisi.swoopdserver.users.User;

import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@Accessors(chain = true)
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    @ManyToOne
    @JoinColumn(name =  "userId")
    private User user;

    @Column
    private String vehicleRegNumber;
    @Column
    private String vehicleDescription;
}
