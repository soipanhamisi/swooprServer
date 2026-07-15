package org.hamisi.swoopdserver.tripManagement.repositories;

import jakarta.persistence.LockModeType;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideSeekerBacklogRepository extends JpaRepository<RideSeekerBacklogEntry, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RideSeekerBacklogEntry> findByMatchedFalseOrderByRequestMadeAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RideSeekerBacklogEntry> findFirstByMatchedFalseAndDestinationZoneIgnoreCaseOrderByRequestMadeAtAsc(String destinationZone);

    List<RideSeekerBacklogEntry> user(User user);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM ride_seeker_backlog r WHERE r.user_id = :userId)", nativeQuery = true)
    boolean isInBackLog(@Param("userId") UUID userId);
}
