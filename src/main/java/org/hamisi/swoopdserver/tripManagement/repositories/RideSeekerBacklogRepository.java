package org.hamisi.swoopdserver.tripManagement.repositories;

import jakarta.persistence.LockModeType;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RideSeekerBacklogRepository extends JpaRepository<RideSeekerBacklogEntry, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RideSeekerBacklogEntry> findByMatchedFalseOrderByRequestMadeAtAsc();

    List<RideSeekerBacklogEntry> user(User user);

    @Query("SELECT COUNT(r) > 0 FROM RideSeekerBacklogEntry r WHERE r.user.userId = :userId")
    boolean isInBackLog(@Param("userId") UUID userId);
    @Query("SELECT r FROM RideSeekerBacklogEntry r WHERE r.user.userId = :userId" +
            " AND r.matched = false")
    RideSeekerBacklogEntry getUserBacklogEntry(UUID userId);
}
