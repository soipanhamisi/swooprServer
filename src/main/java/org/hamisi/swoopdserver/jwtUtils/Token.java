package org.hamisi.swoopdserver.jwtUtils;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int tokenId;
    @Column
    private String token;
    @Column
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column
    private LocalDateTime expiresAt = LocalDateTime.now().plusWeeks(1);

    public Token(String tokenString) {
        this.token = tokenString;
    }
}
