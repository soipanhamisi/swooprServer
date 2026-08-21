package org.hamisi.swoopdserver.tripManagement.services;

import lombok.extern.slf4j.Slf4j;
import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService;
import org.hamisi.swoopdserver.tripManagement.entities.OriginDestination;
import org.hamisi.swoopdserver.tripManagement.entities.RideSeekerBacklogEntry;
import org.hamisi.swoopdserver.tripManagement.exceptions.CannotCreateCarpoolRequestException;
import org.hamisi.swoopdserver.tripManagement.repositories.RideSeekerBacklogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BacklogManagementService {
    private final RideSeekerBacklogRepository rideSeekerBacklogRepository;
    private final UsersRepository usersRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    public BacklogManagementService(RideSeekerBacklogRepository rideSeekerBacklogRepository, UsersRepository usersRepository, FirebaseMessagingService firebaseMessagingService) {
        this.rideSeekerBacklogRepository = rideSeekerBacklogRepository;
        this.usersRepository = usersRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    /**
     * Checks that the user does not already have an active backlog request.
     * Resolves origin and destination zone data for future matching comparisons.
     * Persists a new backlog entry with a PENDING status and the current request timestamp.
     * */
    public void createBacklogRequest(
            UUID userId,
            LocalDateTime departureTime,
            OriginDestination originDestinationCoordinatePair
    ){
        if (rideSeekerBacklogRepository.isInBackLog(userId, departureTime)){
            throw new CannotCreateCarpoolRequestException("User already in backlog");
        }
        RideSeekerBacklogEntry backlogEntry = new RideSeekerBacklogEntry();
        backlogEntry.setMatched(false)
                .setRequestMadeAt(LocalDateTime.now())
                .setSelectedDepartureTime(departureTime)
                .setUser(usersRepository.getReferenceById(userId))
                .setOriginDestinationCoordinatePair(originDestinationCoordinatePair);

        rideSeekerBacklogRepository.save(backlogEntry);
    }

    public List<RideSeekerBacklogEntry> getActiveBacklogRequests(){
        return rideSeekerBacklogRepository.findByMatchedFalseOrderByRequestMadeAtAsc();
    }

    public void cancelBacklogRequest(UUID userId){
        if (!rideSeekerBacklogRepository.isInBackLog(userId)){
            throw new RuntimeException("User not in backlog");
        }
        RideSeekerBacklogEntry rideSeekerBacklogEntry = rideSeekerBacklogRepository.getUserBacklogEntry(userId);
        rideSeekerBacklogRepository.delete(rideSeekerBacklogEntry);
    }

    public void markAsMatched(UUID backLogEntryId, LocalDateTime matchedAt){
        RideSeekerBacklogEntry rideSeekerBacklogEntry = rideSeekerBacklogRepository.getReferenceById(backLogEntryId);
        rideSeekerBacklogEntry.setMatched(true);
        rideSeekerBacklogRepository.save(rideSeekerBacklogEntry);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${trip.backlog.cleanup.delay-ms}")
    public void expireStaleRequests(){
        List<RideSeekerBacklogEntry> expiredEntries = rideSeekerBacklogRepository.findByMatchedFalseAndSelectedDepartureTimeBefore(LocalDateTime.now());
        for (RideSeekerBacklogEntry backlogEntry: expiredEntries){
            messageUser(backlogEntry.getUser().getUserId());
            rideSeekerBacklogRepository.delete(backlogEntry);
        }
        log.debug("{} - stale requests expired from database", expiredEntries.size());
    }

    public boolean hasBacklogRequest(UUID userId){
        return rideSeekerBacklogRepository.getUserBacklogEntry(userId) != null;
    }
    private void messageUser(UUID userId) {
        firebaseMessagingService.sendNotification(
                userId,
                "BACKLOG_MANAGEMENT_SERVICE",
                "REQUEST_EXPIRED",
                "Could not find suitable trip before requested time departure Time");
    }

}
