package org.hamisi.swoopdserver.tripManagement.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "ride_seeker_backlog",
        indexes = {
                @Index(name = "idx_backlog_match_zone_requested", columnList = "matched,destinationZone,requestMadeAt"),
                @Index(name = "idx_backlog_user_match", columnList = "userId,matched")
        }
)
@Getter
@Setter
public class RideSeekerBacklogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID backlogEntryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false)
    private String destinationZone;

    @Column(nullable = false)
    private LocalDateTime requestMadeAt;

    @Column(nullable = false)
    private boolean matched;

    @Column
    private LocalDateTime matchedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (requestMadeAt == null) {
            requestMadeAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

