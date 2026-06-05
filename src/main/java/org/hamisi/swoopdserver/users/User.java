package org.hamisi.swoopdserver.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users_tbl")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String userId;

    @Column
    private String fullName;

    @Column
    private String email;

    @Column
    private String password;

    @Column
    private Role role;

}
