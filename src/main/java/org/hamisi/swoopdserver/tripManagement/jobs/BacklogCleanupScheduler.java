package org.hamisi.swoopdserver.tripManagement.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.tripManagement.services.TripManagementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BacklogCleanupScheduler {

    private final TripManagementService tripManagementService;

    @Scheduled(fixedDelayString = "${trip.backlog.cleanup.delay-ms:60000}")
    public void cleanupExpiredRideSeekers(){
        int removed = tripManagementService.expireStaleBacklogEntries();
        if (removed > 0){
            log.info("Expired {} stale backlog ride seeker request(s)", removed);
        }
    }
}
