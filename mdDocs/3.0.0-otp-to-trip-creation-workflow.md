# Swoopd: OTP to Trip Creation Workflow
**Live GCP Base URL:** `https://swooprserver-373496068484.europe-west1.run.app`

---

## Quick Workflow Overview

```
1. User enters email
   ↓
2. Request OTP → Email sent
   ↓
3. User enters OTP code
   ↓
4. Verify OTP → Get JWT token
   ↓
5. Register/Save user profile
   ↓
6. Register vehicle (if host)
   ↓
7. Create trip (if host) OR join carpool (if seeker)
   ↓
8. Monitor real-time notifications (FCM)
```

---

## 1. AUTHENTICATION FLOW

### 1.1 Request OTP

**Endpoint:** `POST /auth/getOtp`

**Request:**
```json
{
  "email": "student@usiu.ac.ke"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent to email",
  "data": null
}
```

**Purpose:** Sends one-time password to student email  
**OTP Valid For:** ~10-15 minutes  
**Requirements:** Valid USIU email only

---

### 1.2 Verify OTP & Get JWT Token

**Endpoint:** `POST /auth/getNewToken`

**Request:**
```json
{
  "email": "student@usiu.ac.ke",
  "otp": 123456
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NDMwMDAwMC04ZjAzLTRhZmEtOGE1My00YmY3MzdiMDc2YzQiLCJleHAiOjE3NTI1MzA0MDIsImlhdCI6MTc1MTkyNTYwMn0.Rg7h3w...",
    "expiresIn": 604800,
    "tokenType": "Bearer"
  }
}
```

**Token Details:**
- **Valid for:** 7 days (604800 seconds)
- **Type:** Bearer
- **Usage:** Include in all protected endpoint headers
- **Storage:** Save securely (EncryptedSharedPreferences or Android Keystore)

---

### 1.3 Refresh Token (Before Expiration)

**Endpoint:** `POST /auth/refreshToken`

**Request:**
```json
{
  "email": "student@usiu.ac.ke"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 604800,
    "tokenType": "Bearer"
  }
}
```

**When to use:** Refresh token 1 day before expiration to maintain uninterrupted access

---

## 2. USER REGISTRATION

### 2.1 Save User Profile

**Endpoint:** `POST /auth/saveUser`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+254712345678",
  "email": "john.doe@usiu.ac.ke",
  "messagingToken": "eVjVKBpNvqk:APA91bG_xyz..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User saved successfully",
  "data": {
    "id": "543aaaaa-8f03-4afa-8a53-4bf737b076c4",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@usiu.ac.ke",
    "phoneNumber": "+254712345678",
    "messagingToken": "eVjVKBpNvqk:APA91bG_xyz..."
  }
}
```

**Field Details:**
- **firstName, lastName:** User's name
- **phoneNumber:** Mobile number for contact
- **email:** USIU email (verified via OTP)
- **messagingToken:** FCM token for push notifications

---

## 3. HOST WORKFLOW: CREATE CARPOOL TRIP

### 3.1 Register Vehicle

**Endpoint:** `POST /trips/registerVehicle`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "make": "Toyota",
  "model": "Prado",
  "year": 2021,
  "licensePlate": "KBZ 123AB",
  "seatingCapacity": 4,
  "color": "White"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Vehicle registered successfully",
  "data": {
    "id": "vehicle-uuid-123",
    "make": "Toyota",
    "model": "Prado",
    "year": 2021,
    "licensePlate": "KBZ 123AB",
    "seatingCapacity": 4,
    "color": "White"
  }
}
```

---

### 3.2 Create Trip

**Endpoint:** `POST /trips/createTrip`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "originLatitude": -1.2607,
  "originLongitude": 36.8161,
  "destinationLatitude": -1.3200,
  "destinationLongitude": 36.7762,
  "departureTime": "2026-07-11T14:30:00Z",
  "vehicleId": "vehicle-uuid-123",
  "costPerPassenger": 300,
  "notes": "Highway route, comfortable AC"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Trip created successfully",
  "data": {
    "tripId": "trip-uuid-456",
    "hostId": "user-uuid-789",
    "hostName": "John Doe",
    "hostPhone": "+254712345678",
    "originLatitude": -1.2607,
    "originLongitude": 36.8161,
    "destinationLatitude": -1.3200,
    "destinationLongitude": 36.7762,
    "departureTime": "2026-07-11T14:30:00Z",
    "costPerPassenger": 300,
    "status": "OPEN",
    "availableSeats": 3,
    "totalSeats": 4,
    "createdAt": "2026-07-11T10:00:00Z"
  }
}
```

**Field Details:**
- **originLatitude/Longitude:** User clicked location on map
- **destinationLatitude/Longitude:** User clicked destination on map
- **departureTime:** ISO 8601 format
- **costPerPassenger:** KES amount per passenger
- **status:** Initially "OPEN"
- **availableSeats:** Total capacity minus 1 (host seat)

---

## 4. SEEKER WORKFLOW: JOIN CARPOOL

### 4.1 Search & Join Trip

**Endpoint:** `POST /trips/joinCarPool`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "originLatitude": -1.2607,
  "originLongitude": 36.8161,
  "destinationLatitude": -1.3200,
  "destinationLongitude": 36.7762,
  "departureTime": "2026-07-11T14:30:00Z"
}
```

**Response Scenario A: Match Found (200 OK)**
```json
{
  "success": true,
  "message": "Successfully joined carpool",
  "data": {
    "tripId": "trip-uuid-456",
    "hostId": "host-uuid-111",
    "hostName": "John Doe",
    "hostPhone": "+254712345678",
    "status": "MATCHED",
    "availableSeats": 2,
    "totalSeats": 4,
    "departureTime": "2026-07-11T14:30:00Z",
    "costPerPassenger": 300,
    "matchedAt": "2026-07-11T10:05:00Z"
  }
}
```

**Response Scenario B: No Match - Added to Backlog (202 Accepted)**
```json
{
  "success": true,
  "message": "No matching trip found. Added to backlog queue",
  "data": {
    "backlogId": "backlog-uuid-789",
    "status": "BACKLOG",
    "originLatitude": -1.2607,
    "originLongitude": 36.8161,
    "destinationLatitude": -1.3200,
    "destinationLongitude": 36.7762,
    "departureTime": "2026-07-11T14:30:00Z",
    "addedAt": "2026-07-11T10:05:00Z",
    "message": "You will be notified when a matching trip is available"
  }
}
```

**Matching Logic:**
- System searches for trips with SAME destination zone
- Checks within ±30 minute departure time window
- If match found → User added to trip, gets notification
- If no match → User added to backlog, waits for new trips in that zone

---

## 5. HOST WORKFLOW: CANCEL TRIP

**Endpoint:** `POST /trips/cancelTrip`

**Headers:**
```
Authorization: Bearer {token}
```

**Request:**
No request body required. Endpoint identifies host from JWT token and cancels their OPEN trip.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Trip cancelled successfully",
  "data": {
    "tripId": "trip-uuid-456",
    "status": "CANCELLED",
    "cancelledAt": "2026-07-11T10:30:00Z",
    "passengersNotified": 3
  }
}
```

**Error Response (403 Forbidden):**
```json
{
  "success": false,
  "message": "Cannot cancel trip. No open trip found or trip already cancelled",
  "error": "FORBIDDEN",
  "statusCode": 403
}
```

**Side Effects:**
- All passengers in trip notified via FCM
- Passengers added to backlog for automatic future matching
- Trip removed from available listings

---

## 6. REAL-TIME NOTIFICATIONS (FCM)

### 6.1 Notification Types

**Trip Matched Notification:**
```json
{
  "title": "Carpool Match Found!",
  "body": "John Doe is heading to Westlands. Join now!",
  "type": "TRIP_MATCHED",
  "tripId": "trip-uuid-456",
  "hostName": "John Doe",
  "hostPhone": "+254712345678",
  "departureTime": "2026-07-11T14:30:00Z"
}
```

**Passenger Joined Notification (to host):**
```json
{
  "title": "New Passenger!",
  "body": "Jane Smith joined your trip to Westlands",
  "type": "PASSENGER_JOINED",
  "tripId": "trip-uuid-456",
  "passengerName": "Jane Smith",
  "availableSeats": 2
}
```

**Trip Cancelled Notification (to passengers):**
```json
{
  "title": "Trip Cancelled",
  "body": "John Doe cancelled the trip to Westlands",
  "type": "TRIP_CANCELLED",
  "tripId": "trip-uuid-456",
  "cancellationReason": "Vehicle breakdown"
}
```

---

## 7. ERROR CODES & RESPONSES

| Status | Scenario | Example |
|--------|----------|---------|
| **200** | Success | OTP verified, trip created, user joined |
| **201** | Created | Trip successfully created |
| **202** | Accepted | Seeker added to backlog (no match) |
| **400** | Bad Request | Invalid email, missing fields, malformed JSON |
| **401** | Unauthorized | Token expired, invalid token, missing auth header |
| **403** | Forbidden | Non-USIU email, email not verified |
| **404** | Not Found | Trip doesn't exist, vehicle doesn't exist |
| **409** | Conflict | Trip already full, vehicle already registered |
| **500** | Server Error | Database error, external API failure |

**Example Error Response:**
```json
{
  "success": false,
  "message": "Token expired. Please refresh or login again",
  "error": "UNAUTHORIZED",
  "statusCode": 401
}
```

---

## 8. IMPLEMENTATION CHECKLIST

### Authentication Setup
- [ ] Display email input screen
- [ ] Call `/auth/getOtp` when user submits email
- [ ] Display OTP input screen
- [ ] Call `/auth/getNewToken` when user submits OTP
- [ ] Store JWT token securely
- [ ] Add `Authorization: Bearer {token}` to all subsequent requests

### User Registration
- [ ] Display user profile form (firstName, lastName, phone)
- [ ] Get FCM token from Firebase
- [ ] Call `/auth/saveUser` with profile + FCM token

### Host Flow
- [ ] Display vehicle registration form
- [ ] Call `/trips/registerVehicle`
- [ ] Display map UI for origin/destination selection
- [ ] Call `/trips/createTrip` with map coordinates
- [ ] Listen for FCM notifications (passenger joined, cancellations)

### Seeker Flow
- [ ] Display map UI for origin/destination selection
- [ ] Call `/trips/joinCarPool` with map coordinates
- [ ] Handle two response types:
  - **Matched** → Show trip details, host info
  - **Backlog** → Show "waiting for match" message
- [ ] Listen for FCM notifications (trip matched, trip cancelled)

---

## 9. TESTING THE ENDPOINTS

### Using cURL

```bash
# 1. Request OTP
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/auth/getOtp \
  -H "Content-Type: application/json" \
  -d '{"email":"student@usiu.ac.ke"}'

# 2. Verify OTP (replace TOKEN with actual JWT)
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/auth/getNewToken \
  -H "Content-Type: application/json" \
  -d '{"email":"student@usiu.ac.ke","otp":123456}'

# 3. Save User Profile (include token)
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/auth/saveUser \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"John",
    "lastName":"Doe",
    "phoneNumber":"+254712345678",
    "email":"student@usiu.ac.ke",
    "messagingToken":"FCM_TOKEN_HERE"
  }'

# 4. Register Vehicle
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/trips/registerVehicle \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "make":"Toyota",
    "model":"Prado",
    "year":2021,
    "licensePlate":"KBZ 123AB",
    "seatingCapacity":4,
    "color":"White"
  }'

# 5. Create Trip
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/trips/createTrip \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "originLatitude":-1.2607,
    "originLongitude":36.8161,
    "destinationLatitude":-1.3200,
    "destinationLongitude":36.7762,
    "departureTime":"2026-07-11T14:30:00Z",
    "vehicleId":"YOUR_VEHICLE_ID",
    "costPerPassenger":300,
    "notes":"Comfortable AC"
  }'

# 6. Join Carpool
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/trips/joinCarPool \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "originLatitude":-1.2607,
    "originLongitude":36.8161,
    "destinationLatitude":-1.3200,
    "destinationLongitude":36.7762,
    "departureTime":"2026-07-11T14:30:00Z"
  }'

# 7. Cancel Trip (no request body, token only)
curl -X POST https://swooprserver-373496068484.europe-west1.run.app/trips/cancelTrip \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 10. IMPORTANT NOTES

✅ **Map Coordinates:** Latitude/Longitude from map UI clicks go directly to OTP and trip creation endpoints

✅ **FCM Integration:** Already working - tokens are sent with `/auth/saveUser` and used for notifications

✅ **Token Management:** Store token securely, refresh before 7-day expiration

✅ **Error Handling:** Always check `success` field in response, not just HTTP status code

✅ **Backlog Flow:** Automatic - when new trips created, system auto-matches backlog users

✅ **Real-Time Updates:** Use FCM for notifications, poll `/trips/getTripDetails` for status if needed

✅ **Map Coordinates Format:** Standard latitude/longitude decimal degrees (e.g., -1.2607)

---

**Last Updated:** July 11, 2026  
**Status:** Live on GCP Cloud Run  
**Base URL:** `https://swooprserver-373496068484.europe-west1.run.app`

