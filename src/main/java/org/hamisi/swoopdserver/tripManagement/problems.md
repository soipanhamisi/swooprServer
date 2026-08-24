Got it — I reviewed the `tripManagement` services with a logic-focused code review.

- [x] Scanned core service classes under `tripManagement/services`
- [x] Identified high-impact logic bugs and behavioral risks
- [x] Mapped each finding to exact file/line locations
- [x] Noted test gaps that could hide regressions

**Findings (ordered by severity)**

~~- **Critical: time-window filtering is inverted, likely excluding valid backlog riders**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/CarpoolMatchingService.java:95-98`, `earliest` is set to `departure + 15` and `latest` to `departure - 15`; those bounds are reversed.
    - Current predicate removes entries `isBefore(earliest) || isAfter(latest)`, which with reversed bounds can remove almost everything.
    - Expected logic is usually window `[departure-15, departure+15]`, then remove outside that window.~~

~~- **Critical: `VehicleDto` reuse bug returns duplicated/incorrect vehicle list**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/VehicleManagementService.java:61-66`, one `VehicleDto` object is created outside the loop and reused.
    - Result: returned list contains multiple references to the same object (all rows look like the last vehicle).~~

~~- **High: duplicate rider can be added to trip without membership guard**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/CarpoolMatchingTxService.java:44-51`, user is always added to `matchedTrip` and a new `TripMembership` is always saved.
    - If user is already in the trip (race/retry/duplicate request), this can create duplicate association or DB integrity failure depending on constraints.
    - There is no explicit check like “trip already contains user”.~~

~~ - **High: trip creation can onboard backlog users but not persist membership changes reliably**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/TripLifecycleManagementService.java:147-149`, you save trip, then call `carpoolMatchingService.onBoardBackloggedUsers(savedTrip)`.
    - In `onBoardBackloggedUsers`, users are added to `trip` in memory, but there is no explicit save of the updated trip in that method (`CarpoolMatchingService.java:110-121`).
    - Depending on transaction boundaries (especially because `createTrip` is `@Async` and not `@Transactional`), onboarded users may not persist consistently.

- **Medium: `markAsMatched` ignores `matchedAt` parameter**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/BacklogManagementService.java:66-69`, method takes `matchedAt` but never sets it.
    - That causes stale/incorrect audit info.

- **Medium: backlog cancellation notification payload differs from typed event usage**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/TripLifecycleManagementService.java:244-249`, a raw string is sent where elsewhere you use `TripLifeCycleManagementEvent`.
    - If clients expect structured payload, this one path can break parsing/UX consistency.~~

- **Medium: cancellation path likely misses departure time for backlog requeue**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/TripLifecycleManagementService.java:239-243`, `preferredDepartureTime` from `TripMembership` is reused.
    - Host membership in `createTrip` does not set preferred departure time; rider membership in `CarpoolMatchingTxService` does.
    - For members whose preferred time is null, `createBacklogRequest` gets null and could break later comparisons/queries.

- **Low: strict double equality for min-distance match**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/PolylineProximityEvaluator.java:31`, equality uses `==` on doubles.
    - Usually safer to compare with epsilon; this may produce unstable tie behavior.

- **Low: `hasBacklogRequest` relies on nullable query return**
    - In `src/main/java/org/hamisi/swoopdserver/tripManagement/services/BacklogManagementService.java:84`, it uses `getUserBacklogEntry(userId) != null`.
    - If repository throws on no row (or on duplicates), behavior differs unexpectedly; explicit `exists...` query is safer. ~~