package org.hamisi.swoopdserver.tripManagement.services;

import org.hamisi.swoopdserver.auth.repository.UsersRepository;
import org.hamisi.swoopdserver.tripManagement.CannotCreateTripException;
import org.hamisi.swoopdserver.tripManagement.dtos.TripCreationDTO;
import org.hamisi.swoopdserver.tripManagement.entities.Trip;
import org.hamisi.swoopdserver.tripManagement.entities.TripStatus;
import org.hamisi.swoopdserver.tripManagement.proxies.GoogleRoutesProxy;
import org.hamisi.swoopdserver.tripManagement.repositories.TripRepository;
import org.hamisi.swoopdserver.users.Role;
import org.springframework.stereotype.Service;

@Service
public class TripManagementService {
    private final TripRepository tripRepository;
    private final UsersRepository usersRepository;
    private final GoogleRoutesProxy googleRoutesProxy;

    public TripManagementService(TripRepository tripRepository, UsersRepository usersRepository, GoogleRoutesProxy googleRoutesProxy) {
        this.tripRepository = tripRepository;
        this.usersRepository = usersRepository;
        this.googleRoutesProxy = googleRoutesProxy;
    }

    public void createTrip(TripCreationDTO tripCreationDTO  ) {
        if(!isPermitted(tripCreationDTO.getEmail())){
            throw new CannotCreateTripException("User is not permitted to create a trip");
        }
        Trip trip = new Trip();
        trip.setTripCapacity(tripCreationDTO.getTripCapacity());
        trip.setTripStatus(TripStatus.OPEN);
        trip.setDepartureTime(tripCreationDTO.getDepartureTime());
        trip.setRoutePolyline(googleRoutesProxy.getRoute(
                tripCreationDTO.getDestinationLongitude(),
                tripCreationDTO.getDestinationLatitude(),
                tripCreationDTO.getOriginLongitude(),
                tripCreationDTO.getOriginLatitude()));
        tripRepository.save(trip);
    }

    public void cancelTrip(String email){
        if(!isPermitted(email)){
            throw new CannotCreateTripException("User is not permitted to delete a trip");
        }
        tripRepository.cancelTrip(email);
    }

    private boolean isPermitted(String email) {
        return usersRepository.findByEmail(email)
                .map(user -> user.getRole() == Role.CARPOOL_HOST)
                .orElse(false);
    }

}
