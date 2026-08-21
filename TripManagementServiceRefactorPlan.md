# TripManagementService Full Refactor

**Problem:** The `TripManagementService` is too large and is a verbose nightmare to debug.

**Solution:** The service will be broken down into 6 smaller, focused services:

1. `TripLifecycleManagementService`
2. `CarpoolMatchingService`
3. `BacklogManagementService`
4. `VehicleManagementService`
5. `TripRoutingService`
6. `TripNotificationService`

---

## 1. TripLifecycleManagementService

### 1.1 Responsibility

The `TripLifecycleManagementService` is responsible for keeping track of trips throughout their individual lifecycles. The trip lifecycle entails the potential states a trip will assume from `OPEN` to `COMPLETED` or `CANCELLED`.

Trip states include: `OPEN`, `FULL`, `IN_PROGRESS`, `CANCELLED`, `COMPLETED`.

1. **OPEN:** The default status of a trip following creation, where the number of users is less than the trip capacity. This state can also be re-assumed when a ride seeker opts out and the user count drops below capacity.
2. **FULL:** Assumed when the user count in the trip equals the trip capacity.
3. **IN_PROGRESS:** Assumed when the departure time has arrived. Both `OPEN` and `FULL` trips can transition to this state.
4. **CANCELLED:** Assumed when the carpool host — and only the carpool host — chooses to cancel an `OPEN` or `FULL` trip.
5. **COMPLETED:** Assumed when the carpool host arrives at their destination. The preceding state must be `IN_PROGRESS`.

---

### 1.2 Service Methods

#### 1. `void createTrip(UUID userId, OriginDestinationCoordinates originDestination, VehicleDto registeredVehicle)`

**Params:** `userId UUID`, `originDestination OriginDestinationCoordinates`, `registeredVehicle VehicleDto`

**What it does:**
- Checks validity of the registered vehicle (i.e. the vehicle has already been persisted in the system).
- Checks whether the user already has an `OPEN`, `FULL`, or `IN_PROGRESS` trip.
- Uses the Google Maps API to resolve origin and destination neighbourhood zones (important for matching).
- Uses the Google Routes API to obtain the route polyline from origin to destination (important for more refined matching, to be implemented later).
 **Add this to prevent check-then-act race conditions**

```sql
ALTER TABLE trips
  ADD COLUMN active_user_id CHAR(36)
  GENERATED ALWAYS AS (
    CASE
      WHEN trip_status IN ('OPEN', 'FULL', 'IN_PROGRESS') THEN trips.active_user_id
      ELSE NULL
    END
  ) STORED,
  ADD UNIQUE INDEX ux_trips_one_active_per_user (active_user_id);
  ```
---

#### 2. `void cancelTrip(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Checks that the user has at least one `OPEN` or `FULL` trip.
- Checks that the user is the creator of that carpool.
- If conditions are met, notifies all trip members of the cancellation.
- Marks the trip as `CANCELLED`.

---

#### 3. `TripDto getTripInfo(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Returns trip info for any `OPEN`, `FULL`, or `IN_PROGRESS` trip of which the user with the given UUID is a member.

---

## 2. CarpoolMatchingService

### 2.1 Responsibility

The `CarpoolMatchingService` is responsible for pairing ride seekers with open carpools based on compatibility of origin zone, destination zone, and desired departure time. It handles the full matching flow: from validating the ride seeker's request and attempting to find a suitable trip, to placing unmatched seekers into a backlog for later matching when a new trip becomes available.

The service does not own trip lifecycle state, vehicle validation, backlog lifecycle scheduling, or notification delivery.

---

### 2.2 Service Methods

#### 1. `Void matchRiderOrBacklog(UUID userId, LocalDateTime desiredDepartureTime, OriginDestination originDestination)`

**Params:** `userId UUID`, `desiredDepartureTime LocalDateTime`, `originDestination OriginDestination`

**What it does:**
- Validates that the origin and destination coordinates are within or involve the USIU campus geofence.
- Checks that the user is not already a member of an `OPEN` or `FULL` trip and has no active backlog request.
- Resolves origin and destination neighborhood zones from coordinates via the Google Maps API.
- get list of `OPEN` tips within a certain departure window e.g. +-15min of ride seeker defined departure window.

- <b>TODO: Enforce 20-minute minimum lead time for trip departure and ride requests.</b>
- filter out the trips ie inbound/outbound of campus.
- Polyline Matching::
  - take the polyline for a trip
  - use the destination pin to find the shortest euclidean distance  to polyline::
  - define a maximum shortest distance to polyline for matching to happen(6km)
- Filters candidate trips further by origin zone compatibility.
- If a match is found, adds the seeker to the trip membership and persists the change. Notifies all trip members of the updated roster.
- If no match is found, delegates to `BacklogManagementService` to create a backlog entry and returns a `NO_MATCH_BACKLOGGED` outcome.
- this is to be done buy an async job worker with event messages passed to the client on completion of each step.
---

#### 2. `int onboardBackloggedRiders(Trip trip)`

**Params:** `trip Trip`

**What it does:**
- Called after a new trip is created to fill available seats from the backlog.
- Queries the backlog for unmatched entries ordered by request time (oldest first).
- Filters entries whose origin and destination zones are compatible with the new trip's zones.
- Adds compatible seekers to the trip up to the remaining seat capacity.
- Delegates to `BacklogManagementService` to mark each matched backlog entry as matched with a timestamp.
- Returns the number of riders successfully onboarded.

---

#### 3. `boolean isEligibleForMatchRequest(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Returns `true` if the user is not currently a member of any `OPEN`, `FULL`, or `IN_PROGRESS` trip and has no active backlog request.
- Used as a guard by `matchRiderOrBacklog(...)` and can be called independently by the controller layer before initiating a match request.

---

## 3. BacklogManagementService

### 3.1 Responsibility

The `BacklogManagementService` is responsible for the full lifecycle of unmatched ride requests. When a ride seeker 
cannot be matched to an existing trip or a trip is cancelled by the owner, their request is placed in the backlog and held until a compatible
trip is created or the request expires.

The service ensures:
- Backlog entries are created, queried, cancelled, expired, and marked matched in a controlled manner.
- A user cannot have more than one active backlog request at a time.
- Stale backlog entries are expired on a scheduled basis.

The service does not own matching decisions, trip lifecycle state, vehicle data, routing data, or notification delivery.

---

### 3.2 Service Methods

#### 1. `void createBacklogRequest(UUID userId, LocalDateTime desiredDepartureTime, OriginDestinationCoordinates originDestination)`

**Params:** `userId UUID`, `desiredDepartureTime LocalDateTime`, `originDestination OriginDestinationCoordinates`

**What it does:**
- Checks that the user does not already have an active backlog request.
- Resolves origin and destination zone data for future matching comparisons.
- Persists a new backlog entry with a `PENDING` status and the current request timestamp.

**Failure scenarios:**
- User already has an active backlog entry -> domain-specific exception.

---

#### 2. `BacklogEntryDto getActiveBacklogRequest(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Retrieves the user's current active backlog entry if one exists.
- Returns `null` or throws a not-found exception if the user has no active request (based on implementation choice).

**Returns:** `BacklogEntryDto` containing the backlog entry details.

---

#### 3. `void cancelBacklogRequest(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Checks that the user has an active backlog request.
- Marks the entry as `CANCELLED` and persists the change.

**Failure scenarios:**
- No active backlog request found -> domain-specific exception.

---

#### 4. `int expireStaleRequests()`

**What it does:**
- Scheduled operation that scans for backlog entries whose desired departure time has passed.
- Marks all stale entries as `EXPIRED`.
- Delegates to `TripNotificationService` to notify each affected user.
- Returns the number of entries expired in the sweep.

**Notes:**
- Should be idempotent — safe to run multiple times without double-expiring entries.

---

#### 5. `void markAsMatched(UUID backlogEntryId, LocalDateTime matchedAt)`

**Params:** `backlogEntryId UUID`, `matchedAt LocalDateTime`

**What it does:**
- Updates the backlog entry status to `MATCHED` and records the matched timestamp.
- Called by `CarpoolMatchingService` after a rider is successfully onboarded to a trip.

---

#### 6. `boolean hasActiveBacklogRequest(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Returns `true` if the user currently has a `PENDING` backlog request.
- Used as a guard in matching and backlog creation flows.

---

## 4. VehicleManagementService

### 4.1 Responsibility

The `VehicleManagementService` is responsible for managing user vehicles used for carpool hosting. Its core responsibility is to register, validate, and retrieve vehicles that belong to users.

The service ensures:
- Only valid registration numbers are accepted.
- Duplicate vehicle registration numbers are not persisted.
- Vehicles are correctly associated with their owning user.
- Trip creation can rely on persisted vehicle records owned by the requesting host.

---

### 4.2 Service Methods

#### 1. `void registerVehicle(UUID userId, VehicleDto vehicleDto)`

**Params:** `userId UUID`, `vehicleDto VehicleDto`

**What it does:**
- Normalizes the provided registration number (trim, lowercase, remove spaces).
- Validates the number plate format based on current system rules (e.g. Kenyan-style format pattern).
- Checks whether the registration number already exists in the system.
- Fetches the user by `userId` and associates the vehicle with that user.
- Persists the vehicle record if all checks pass.

**Validation rules:**
- Registration number must be present and correctly formatted.
- Registration number must be globally unique.
- User must exist before persistence.

**Failure scenarios:**
- Invalid registration format -> `RegisterVehicleException`
- Vehicle already registered -> `RegisterVehicleException`
- User not found -> domain-specific validation exception

---

#### 2. `List<VehicleDto> getRegisteredVehicles(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Retrieves all persisted vehicles linked to the given user.
- Maps each vehicle entity to a `VehicleDto` for API/service consumption.
- Returns an empty list if no vehicles are registered for the user.

**Returns:** `List<VehicleDto>` containing `regNo` and `desc` for each registered vehicle.

---

#### 3. `boolean isVehicleOwnedByUser(UUID userId, String regNo)`

**Params:** `userId UUID`, `regNo String`

**What it does:**
- Normalizes the incoming registration number.
- Loads the user's registered vehicles and checks ownership match.
- Returns `true` only if the vehicle exists and belongs to the user.

**Usage:** Used by the trip lifecycle flow to confirm the host is creating a trip with a vehicle they own.

---

#### 4. `String normalizeRegNumber(String regNo)`

**Params:** `regNo String`

**What it does:**
- Standardizes registration numbers for consistent storage and comparison.
- Typical normalization: trim whitespace, lowercase, remove internal spaces.

**Usage:** Called internally by registration and ownership checks.

---

#### 5. `boolean isValidRegNumberFormat(String regNo)`

**Params:** `regNo String`

**What it does:**
- Validates the registration number structure against the domain regex/pattern rule.
- Returns `true` only when the format is acceptable for persistence.

**Usage:** Internal guard used by `registerVehicle(...)`.

---

## 5. TripRoutingService

### 5.1 Responsibility

The `TripRoutingService` is responsible for route-related data used during trip creation, trip matching, and trip presentation. Its core responsibility is to compute, store, and expose routing information that helps the system understand how a trip travels from origin to destination.

This service focuses on route intelligence rather than trip lifecycle or matching decisions. It interacts with mapping/routing providers such as the Google Routes API to obtain route geometry and travel details.

Typical routing data includes:
- Route polyline
- Estimated travel distance and time
- Route waypoints or path metadata
- Origin and destination neighbourhood/zone information needed for matching support

The service does not own trip lifecycle state, vehicle validation, backlog handling, or notification delivery.

---

### 5.2 Service Methods

#### 1. `TripRouteDto buildTripRoute(OriginDestinationCoordinates originDestination)`

**Params:** `originDestination OriginDestinationCoordinates`

**What it does:**
- Calls the routing provider to calculate the route between origin and destination.
- Extracts the route polyline and metrics such as distance and duration where available.
- Optionally derives route-related zone or neighbourhood metadata for later matching.

**Returns:** `TripRouteDto` containing route data needed by trip creation and matching.

**Notes:**
- Should fail fast if coordinates are invalid.
- Provider failures should be wrapped in a routing-specific exception.

---

#### 2. `TripRouteDto getTripRoute(UUID tripId)`

**Params:** `tripId UUID`

**What it does:**
- Retrieves the stored route information for the given trip.
- Returns polyline and route metadata for display or downstream use.

**Returns:** `TripRouteDto` if the route exists.

**Notes:**
- Should throw a not-found exception if the trip route has not been generated.
- Should not recalculate the route unless explicitly designed to refresh.

---

#### 3. `TripRouteDto refreshTripRoute(UUID tripId)`

**Params:** `tripId UUID`

**What it does:**
- Loads the trip's latest origin and destination coordinates.
- Recalculates route details from the mapping provider.
- Updates the persisted route data and returns the refreshed result.

**Returns:** Updated `TripRouteDto`.

**Notes:**
- Should validate that the trip exists before recalculation.
- Should be transactional if route persistence is part of the operation.

---

#### 4. `boolean isRouteCompatibleWithRequest(TripRouteDto tripRoute, OriginDestinationCoordinates requestCoordinates)`

**Params:** `tripRoute TripRouteDto`, `requestCoordinates OriginDestinationCoordinates`

**What it does:**
- Checks whether a rider request is sufficiently aligned with the trip route.
- May compare route proximity, zones, or corridor overlap depending on the matching rules.

**Returns:** `true` if the route is compatible, `false` otherwise.

**Notes:**
- Contains only compatibility logic — no persistence.
- Exact compatibility rules may evolve as matching becomes more refined.

---

#### 5. `RouteZoneData deriveRouteZones(OriginDestinationCoordinates originDestination)`

**Params:** `originDestination OriginDestinationCoordinates`

**What it does:**
- Determines the neighbourhood or zone information associated with the trip path.
- Prepares zone data for use by matching and filtering logic.

**Returns:** `RouteZoneData` containing route-related zone metadata.

**Notes:**
- If zone derivation depends on external services, failures should be handled explicitly.

**Typical usage:**
- `TripLifecycleManagementService` calls `buildTripRoute(...)` during trip creation.
- `CarpoolMatchingService` uses `TripRouteDto` and compatibility helpers during rider matching.
- Controllers may call `getTripRoute(...)` to display route details to the user.

---

## 6. TripNotificationService

### 6.1 Responsibility

The `TripNotificationService` is responsible for delivering push notifications to users in response to trip-related events. It acts as the single outbound notification channel for all trip domain events, decoupling notification concerns from business logic in lifecycle, matching, and backlog services.

The service does not make trip decisions — it only reacts to events triggered by other services and dispatches the appropriate message to the appropriate recipients.

Notification events covered:
1. A rider successfully joins a trip — all existing trip members are notified.
2. A trip is cancelled by the carpool host — all trip members are notified.
3. A trip transitions to `IN_PROGRESS` — all trip members are notified.
4. A trip is marked `COMPLETED` — all trip members are notified.
5. A rider is onboarded from the backlog into a trip — the onboarded rider is notified.
6. A rider's backlog request expires without a match — the rider is notified.

The service does not own trip state, matching logic, backlog lifecycle, vehicle data, or routing data.

---

### 6.2 Service Methods

#### 1. `void notifyRiderJoined(Trip trip, UUID joinedUserId)`

**Params:** `trip Trip`, `joinedUserId UUID`

**What it does:**
- Sends a push notification to all current members of the trip informing them that a new rider has joined.
- The notification payload should identify the trip and indicate the updated member count.

**Notes:**
- Called by `CarpoolMatchingService` after a successful match is persisted.
- Should not throw if a single recipient's device token is stale or missing — log and continue.

---

#### 2. `void notifyTripCancelled(Trip trip)`

**Params:** `trip Trip`

**What it does:**
- Sends a push notification to all members of the trip informing them that the trip has been cancelled.
- All members including the host should receive this notification.

**Notes:**
- Called by `TripLifecycleManagementService` after the trip is marked `CANCELLED`.

---

#### 3. `void notifyTripInProgress(Trip trip)`

**Params:** `trip Trip`

**What it does:**
- Sends a push notification to all trip members informing them that the trip has started.

**Notes:**
- Called by `TripLifecycleManagementService` on the `IN_PROGRESS` state transition.

---

#### 4. `void notifyTripCompleted(Trip trip)`

**Params:** `trip Trip`

**What it does:**
- Sends a push notification to all trip members informing them that the trip has been completed.

**Notes:**
- Called by `TripLifecycleManagementService` on the `COMPLETED` state transition.

---

#### 5. `void notifyBacklogOnboarded(UUID userId, Trip trip)`

**Params:** `userId UUID`, `trip Trip`

**What it does:**
- Sends a push notification to the specific user who was pulled from the backlog and matched to a trip.
- Informs the user that a suitable trip has been found for their pending request.

**Notes:**
- Called by `CarpoolMatchingService` after `onboardBackloggedRiders(...)` successfully adds a rider.

---

#### 6. `void notifyBacklogExpired(UUID userId)`

**Params:** `userId UUID`

**What it does:**
- Sends a push notification to the user whose backlog request has expired without being matched.
- Informs the user that no suitable trip was found and their request has been removed.

**Notes:**
- Called by `BacklogManagementService` during the expiry sweep.

---

**Shared failure rules for all notification methods:**
- Notification failures must never propagate back to the calling service or roll back a business transaction.
- Missing or invalid device tokens should be caught, logged, and skipped.
- Each method should be independently safe to call — a failure in one notification must not block others.
