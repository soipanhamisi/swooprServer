# Firebase Message Contracts - Trip Management Service

## Overview

The Trip Management Service sends Firebase Cloud Messaging (FCM) notifications through the `FirebaseMessagingService`. All messages follow a standardized envelope structure with metadata and a serialized payload.

---

## Message Envelope Structure

All Firebase messages sent from the application follow this top-level structure:

```json
{
  "message": {
    "token": "<firebase_messaging_token>",
    "data": {
      "originService": "<service_identifier>",
      "notificationType": "<event_type>",
      "payload": "<json_escaped_serialized_payload>"
    }
  }
}
```

### Envelope Fields

| Field | Type | Description |
|-------|------|-------------|
| `message.token` | string | Firebase Cloud Messaging token for the recipient device |
| `message.data.originService` | string | Service identifier (e.g., "TripManagementService") |
| `message.data.notificationType` | string | Type of notification event |
| `message.data.payload` | string | JSON-serialized payload (escaped for safe embedding in JSON) |

---

## Notification Types and Payload Contracts

### 1. TRIP_CANCELLED

**Trigger:** When a carpool host cancels an open trip

**Source:** `TripManagementService.cancelTrip()` (Line 156-161)

**Payload Shape:**
```json
{
  "message": "Your trip has been cancelled by carpool host. You have been placed in a backlog and will be notified if another trip is available"
}
```

**When Sent:**
- Trip status changes from `OPEN` to `CANCELLED`
- Notification sent to all passengers currently in the trip
- Passengers are automatically added to the backlog for the same destination zone

**Example FCM Message:**
```json
{
  "message": {
    "token": "device_token_abc",
    "data": {
      "originService": "TripManagementService",
      "notificationType": "TRIP_CANCELLED",
      "payload": "{\"message\":\"Your trip has been cancelled by carpool host. You have been placed in a backlog and will be notified if another trip is available\"}"
    }
  }
}
```

---

### 2. TRIP_UPDATES


**Trigger:** After any successful trip modification (user joins, backlog users are onboarded, or trip is cancelled)

**Source:** `TripManagementService.updateTripUsers()` (Line 270-276)

**Primary Use Cases:**
- When a new user successfully joins an open carpool (replaces former `CARPOOL_JOINED` notification)
- When backlogged users are matched and added to a newly created trip (replaces former `CARPOOL_MATCHED` notification)
- After any change to trip membership composition

**Payload Shape (sanitized, no internal IDs):**
```json
{
  "tripCapacity": "<integer>",
  "departureTime": "<ISO_8601_datetime>",
  "originDestination": {
    "originLatitude": "<double>",
    "originLongitude": "<double>",
    "destinationLatitude": "<double>",
    "destinationLongitude": "<double>"
  },
  "tripStatus": "OPEN|COMPLETED|CANCELLED",
  "destinationZone": "<zone_string>",
  "routePolyline": "<encoded_polyline_string>",
  "carpoolMemberNames": ["<full_name>"]
}
```

**When Sent:**
- Sent to ALL members of the trip after any carpool composition change
- Contains complete trip information and current membership list
- Payload is a sanitized `TripUpdateNotification` DTO serialized to JSON
- Consolidates former `CARPOOL_JOINED` and `CARPOOL_MATCHED` notifications into a single comprehensive update
- Excludes internal identifiers (`tripId`, `createdBy`, and nested `userId`)

**Example FCM Message:**
```json
{
  "message": {
    "token": "device_token_ghi",
    "data": {
      "originService": "Trip Management Service",
      "notificationType": "TRIP_UPDATES",
      "payload": "{\"tripCapacity\":4,\"departureTime\":\"2026-07-28T15:30:00\",\"originDestination\":{\"originLatitude\":-1.2521,\"originLongitude\":36.7784,\"destinationLatitude\":-1.2688,\"destinationLongitude\":36.8061},\"tripStatus\":\"OPEN\",\"destinationZone\":\"Westlands\",\"routePolyline\":\"encoded_polyline_string\",\"carpoolMemberNames\":[\"Alice Smith\"]}"
    }
  }
}
```

---

### 3. CARPOOL_MATCH_FAILED

**Trigger:** When a backlog entry expires before being matched to a trip

**Source:** `TripManagementService.expireStaleBacklogEntries()` (Line 320-327)

**Payload Shape:**
```json
{
  "message": "We could not find a suitable carpool before your selected departure time. Please request again for a later time."
}
```

**When Sent:**
- Scheduled task checks for backlog entries with `selectedDepartureTime` in the past
- User is notified and removed from backlog if no match was found
- Typically indicates no compatible trips were available during the user's requested timeframe

**Example FCM Message:**
```json
{
  "message": {
    "token": "device_token_jkl",
    "data": {
      "originService": "TripManagementService",
      "notificationType": "CARPOOL_MATCH_FAILED",
      "payload": "{\"message\":\"We could not find a suitable carpool before your selected departure time. Please request again for a later time.\"}"
    }
  }
}
```

---

## Payload Encoding Details

### Serialization Process

1. **Payload Object** → Serialized to JSON string using Jackson `ObjectMapper`
2. **JSON String** → Special characters escaped:
   - `\` → `\\` (backslash doubled)
   - `"` → `\"` (quotes escaped)
3. **Escaped String** → Embedded in the `data.payload` field of FCM message

### Deserialization on Client

To decode received messages, clients should:

1. Extract `payload` string from `data.payload`
2. Unescape JSON characters
3. Parse as JSON object based on `notificationType`

---

## Conditional Sending Rules

### Message Not Sent If:

- **No Messaging Token**: User has not registered a device/token (early return, no error thrown)
- **Database Error**: If token retrieval fails, a `RuntimeException` is thrown and logged

### Recipient Rules by Notification Type:

| Type | Recipients |
|------|-----------|
| `TRIP_CANCELLED` | All passengers in the cancelled trip |
| `TRIP_UPDATES` | All members of the trip (including the person who triggered the change) |
| `CARPOOL_MATCH_FAILED` | Specific backlogged user whose entry expired |

---

## Error Handling

- **Missing Token**: Logged as warning, notification silently skipped
- **Serialization Error**: Throws `RuntimeException`, caught and logged with error context
- **Firebase API Error**: Caught by `FirebaseProxy`, logged and re-thrown

All exceptions are logged with detailed context including user ID and error message.

---

## Refactoring Note: Eliminated Redundant Notifications

**Version 2 - Simplified Message Structure**

Previous versions of this service sent redundant notifications when carpool composition changed:

| Event | Previous Approach | Current Approach |
|-------|-------------------|------------------|
| User joins carpool | `CARPOOL_JOINED` + `TRIP_UPDATES` | `TRIP_UPDATES` only |
| Backlog user matched | `CARPOOL_MATCHED` + `TRIP_UPDATES` | `TRIP_UPDATES` only |

**Rationale:**
- Both `CARPOOL_JOINED` and `CARPOOL_MATCHED` notifications were redundant with `TRIP_UPDATES`
- The `TRIP_UPDATES` message already contains the complete user membership list
- Removing duplicate notifications reduces network traffic and message processing overhead
- Clients now receive a single, comprehensive notification with full trip state

**Impact:**
- Cleaner event flow with single source of truth (the `Trip` object state)
- Reduced notification fatigue for users
- Simpler client implementation (no need to correlate multiple notification types for same event)

---

## Related Classes

- **FirebaseMessagingService** (`org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingService`)
  - Main service handling message construction and sending
  
- **FirebaseProxy** (`org.hamisi.swoopdserver.notificationUtilities.FirebaseProxy`)
  - Communicates with Firebase Cloud Messaging API
  
- **MessagingTokenRepository** (`org.hamisi.swoopdserver.notificationUtilities.MessagingTokenRepository`)
  - Retrieves user device tokens from database

- **TripManagementService** (`org.hamisi.swoopdserver.tripManagement.services.TripManagementService`)
  - Triggers all notifications related to trip lifecycle

---

## Testing Notes

Test files related to Firebase messaging:
- `org.hamisi.swoopdserver.notificationUtilities.FirebaseMessagingServiceTests`
- `org.hamisi.swoopdserver.notificationUtilities.FirebaseProxyTests`
- `org.hamisi.swoopdserver.tripManagement.services.TripManagementServiceTests`

---

**Last Updated:** July 28, 2026

