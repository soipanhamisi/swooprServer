# API Contract - Trip Management Service

## Overview

This document defines the REST API endpoints and Firebase Cloud Messaging (FCM) notification contracts for the Trip Management Service.

---

## REST API Endpoints

All endpoints are under the base path: `/trip-management`

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| POST | `/postTrip` | Create a new carpool trip | Yes |
| POST | `/postRideRequest` | Post a ride request / Join a carpool | Yes |
| POST | `/postVehicle` | Register a new vehicle | Yes |
| POST | `/removeVehicle` | Remove a registered vehicle | Yes |
| POST | `/cancelCarpool` | Cancel a trip (Host) | Yes |
| POST | `/cancelRideRequest` | Cancel a pending ride request | Yes |
| POST | `/leaveCarpool` | Leave an existing carpool trip | Yes |
| GET | `/getTripInfo` | Get detailed information for a trip | Yes |
| GET | `/getRideRequestInfoById` | Get information about a ride request | Yes |
| GET | `/getActiveRideRequests` | Get active ride requests for the user | Yes |
| GET | `/getRegisteredVehicles` | Get vehicles registered by the user | Yes |

### Endpoint Details

*Note: All endpoints require an `Authorization` header with a valid token.*

- **POST /postTrip**
  - Description: Create a new carpool trip.
  - Request Payload: `TripCreationDTO` (includes `capacity`, `departureTime`, `vehicleDto`, `originDestination`).
  - Response: `ApiResponse<Void>`

- **POST /postRideRequest**
  - Description: Post a ride request / Join a carpool (matches with existing trip or adds to backlog).
  - Request Payload: `JoinCarpoolDto` (includes `departureTime`, `rsOriginDestination`).
  - Response: `ApiResponse<Void>`

- **POST /postVehicle**
  - Description: Register a new vehicle.
  - Request Payload: `VehicleDto`.
  - Response: `ApiResponse<Void>`

- **POST /removeVehicle**
  - Description: Remove a registered vehicle.
  - Request Payload: `VehicleDto`.
  - Response: `ApiResponse<Void>`

- **POST /cancelCarpool**
  - Description: Cancel a trip (Host).
  - Request Payload: `String` (tripId).
  - Response: `ApiResponse<Void>`

- **POST /cancelRideRequest**
  - Description: Cancel a pending ride request / backlog entry.
  - Request Payload: None.
  - Response: `ApiResponse<Void>`

- **POST /leaveCarpool**
  - Description: Leave an existing carpool trip.
  - Request Payload: `String` (tripId).
  - Response: `ApiResponse<Void>`

- **GET /getTripInfo**
  - Description: Get detailed information for a trip.
  - Request Payload: `String` (tripId).
  - Response: `ApiResponse<TripData>`

- **GET /getRideRequestInfoById**
  - Description: Get information about a ride request by backlog ID.
  - Request Payload: `String` (backlogId).
  - Response: `ApiResponse<RideSeekerBacklogEntry>`

- **GET /getActiveRideRequests**
  - Description: Get active ride requests for the user.
  - Request Payload: None.
  - Response: `ApiResponse<RideSeekerBacklogEntry>`

- **GET /getRegisteredVehicles**
  - Description: Get vehicles registered by the user.
  - Request Payload: None.
  - Response: `ApiResponse<List<VehicleDto>>`

---

## Firebase Message Contracts

The service sends FCM notifications. For a detailed breakdown of the envelope structure, notification types, and payload contracts, please refer to the `firebaseContracts.md` file in the project root.

### Summary of Notification Types

| Type | Trigger | Recipients |
| :--- | :--- | :--- |
| `TRIP_CANCELLED` | Host cancels an open trip | All passengers in the trip |
| `TRIP_UPDATES` | Membership/Trip composition change | All members of the trip |
| `CARPOOL_MATCH_FAILED` | Backlog entry expires | Specific user |

---

## Error Handling

- API calls return `ApiResponse` objects with success/failure messages.
- HTTP status codes are used to indicate request success (e.g., 200 OK).
- Exceptions are caught and logged server-side, with appropriate error responses returned to the client.

---

**Last Updated:** August 25, 2026
