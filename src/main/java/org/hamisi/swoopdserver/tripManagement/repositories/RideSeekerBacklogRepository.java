package org.hamisi.swoopdserver.tripManagement.repositories;

import jakarta.persistence.LockModeType;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideSeekerBacklogRepository extends JpaRepository<RideSeekerBacklogEntry, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RideSeekerBacklogEntry> findByMatchedFalseOrderByRequestMadeAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RideSeekerBacklogEntry> findFirstByMatchedFalseAndDestinationZoneIgnoreCaseOrderByRequestMadeAtAsc(String destinationZone);
}

