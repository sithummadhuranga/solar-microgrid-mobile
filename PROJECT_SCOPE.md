# Smart Solar Microgrid Trading System: project scope

This file is the single source of truth for the project. Read it fully before writing code. The coding style rules are in SKILL.md.

## How to read this file

Every statement is tagged by where it comes from.

- Sections 2 to 8 come from the assignment document (EAD_SE4040_Assignment_2026.pdf, published 24/08/2026). Treat them as fixed.
- Section 9 lists things the document does not say. Do not pick an answer silently. Ask the user, or use the answer if the user has filled it in.
- Section 13 (auth and roles) mixes document facts and proposals, and each part is labelled.
- Sections 10 to 12 are suggestions made while preparing this file. They are not required by the document. Use them only if the user agrees, and change them when a decision says otherwise.

If something is missing from this file, ask. Do not invent it.

This build is a practice project and is not being submitted. The document has an AI rule for the original assessment (level 2, planning only). It is recorded here as a fact about the document and is not applied to this practice build.

## 1. Summary

An end-to-end Smart Solar Microgrid Trading System using a client-server design. It has three parts.

- A web application for Backoffice administration and microgrid site operators (Grid Operators).
- A mobile application for solar prosumers, who are property owners with solar panel arrays. The mobile app also has an operator mode for Grid Operators.
- A web service (C# Web API) that holds all business logic and talks to a MongoDB database.

```
Web app  (UI only)   ---- REST ---->  Web API on IIS  <---->  MongoDB
Android app (UI only, SQLite) -- REST -->  (all business logic)
```

Both clients talk only to the web service, only through RESTful API calls.

## 2. Actors

| Actor | Uses | What they do (from the document) |
|---|---|---|
| Backoffice | Web app | System administration. Registers microgrid nodes and keeps their schedules. Reactivates deactivated prosumers. |
| Grid Operator | Web app and mobile app | Operational tools. Updates battery slot availability and monitors power trading bookings. In the mobile app, scans prosumer QR codes and finalises the energy transfer. Can help cancel a booking. |
| Prosumer | Mobile app | Registers with NIC, edits profile, requests deactivation, reserves, modifies and cancels energy slots, sees dashboard, history and nearby grid nodes. |

Only Backoffice users have access to system administration functions.

## 3. Requirements

IDs are used in tasks and commits. The last column shows where the marking scheme gives marks for it.

### Web application (W)

| ID | Requirement | Marks area |
|---|---|---|
| W-1 | Web users have two roles, Backoffice and Grid Operator. Backoffice users are the only ones with system administration functions. Grid Operators use operational tools. | Web features: login and role-based access, user management |
| W-2 | Login checks the user type and redirects to the correct home. | Web features |
| W-3 | Prosumer management: create, update and deactivate prosumer profiles. NIC is the primary key. | Web features: user management |
| W-4 | Pending activations are visible and actionable in the web app. | Mobile auth (pending activation view) |
| W-5 | Microgrid node management: create hubs with GPS location, capacity specs (kW/h) and available battery storage slots. Update schedules. Deactivate nodes. Stations and their slots can be created, updated and deleted. | Web features: microgrid node management |
| W-6 | Energy slot reservation management: create, update and cancel power trading reservations. | Web features: slot booking management |
| W-7 | Grid Operators update battery slot availability and monitor power trading bookings (from the project scenario). | Not scored on its own |
| W-8 | UI uses Bootstrap 5, Tailwind CSS or React.js and is responsive. There is a home (index) page. Every page is implemented and working. | UI and experience design |
| W-9 | The web app only calls the Web API. | Client build, service integration |

### Mobile application (M)

| ID | Requirement | Marks area |
|---|---|---|
| M-1 | Pure native Android with a local SQLite database. No cross-platform framework. | Client build |
| M-2 | Login routes each role to the correct home screen (prosumer, grid operator). | Mobile auth |
| M-3 | Prosumers register with NIC as the primary key, edit their profile data and request account deactivation. | Mobile auth |
| M-4 | Prosumers reserve, modify and cancel energy drop-off or charging slots. Updates and cancellations follow the 12-hour rule. A summary page is shown after each action. | Reservation workflow |
| M-5 | Once a booking is approved, the app generates a secure transaction QR code. | Reservation and QR dispatch |
| M-6 | Dashboard shows active and pending reservation counts, bookings, pending reservations and the count of approved future reservations. All values are read live from the API. | Booking views and dashboards |
| M-7 | Users can view booking history, current and pending bookings, and search or filter bookings. | Booking views and dashboards |
| M-8 | Nearby grid nodes are shown with the Google Maps API, plotted from their stored latitude and longitude. Station details show on selection. | Operator and map features |
| M-9 | Operator mode: a Grid Operator logs into the mobile app, scans the prosumer's transaction QR code, verifies it against server data and finalises the energy transfer (job marked done). | Operator and map features |
| M-10 | SQLite stores login details and reference data on the phone. | Service integration and local persistence |
| M-11 | After a booking is confirmed, the prosumer gets a confirmation and can see the details on the dashboard (from the project scenario). | Not scored on its own |

### Web service (S)

| ID | Requirement | Marks area |
|---|---|---|
| S-1 | FAT service pattern. All business logic is in the central API. | Service architecture |
| S-2 | C# Web API deployed on Windows IIS Server and reachable by both clients. | Service architecture (hosting) |
| S-3 | NoSQL server-side database, MongoDB. The connection is stable and used by all endpoints. | Service architecture (MongoDB) |
| S-4 | Both clients communicate only through RESTful API calls. No direct database access from a client. | Client build, service integration |
| S-5 | Four collections with every required field: User's detail, SolarStationInfo, EnergyBookingSlots, Energy Reservation. References between collections are consistent. | Database design |
| S-6 | Sample data is added, manually or through the application. | Database design |

## 4. Business rules

| ID | Rule |
|---|---|
| BR-1 | NIC is the primary key for prosumer profiles, in the web app and the mobile app. |
| BR-2 | A deactivated prosumer account can only be reactivated by a Backoffice officer. |
| BR-3 | A microgrid node cannot be deactivated while active energy reservations exist for it. |
| BR-4 | A reservation must be scheduled within 7 days. |
| BR-5 | Updating or cancelling a reservation needs at least 12 hours' notice. |
| BR-6 | Only Backoffice users can use system administration functions. |
| BR-7 | The QR code is generated only after a booking is approved. |
| BR-8 | When a Grid Operator scans a QR code, the app verifies it against server data before the energy transfer is finalised. |
| BR-9 | Cancellations can be made through the mobile app or with the help of a Grid Operator. |

All of these are enforced in the API (S-1). The clients may also stop obvious mistakes to help the user, but the API is the one that decides.

## 5. Data

The document requires four collections and says each must have "every required field", but it does not list the fields. What follows is only what the document wording implies. The real field lists are a team decision (D-9).

| Collection | Implied by the document |
|---|---|
| User's detail | NIC (primary key for prosumers), role (Backoffice, Grid Operator, prosumer), login details, profile data that a prosumer can edit, active or deactivated state, pending activation state |
| SolarStationInfo | GPS location (latitude and longitude), capacity in kW/h, number of available battery storage slots, schedule, active or deactivated state |
| EnergyBookingSlots | Slots that belong to a station, availability that Grid Operators update |
| Energy Reservation | Prosumer (NIC), station and slot, scheduled time (within 7 days), state (the document mentions pending, approved, cancelled and done), the transaction QR data |

## 6. Screens implied by the requirements

The document has no screen list. These follow from the requirements. Every page must exist and work (UI marks).

Web: login, home (index), web user management, prosumer management, pending activations, microgrid node and slot management, reservation management, operator tools.

Mobile: login, prosumer registration, edit profile, deactivation request, prosumer dashboard, reserve, modify, cancel, summary page after each action, booking history and pending bookings with search, map of nearby nodes, operator home, QR scan and verify, QR code display.

## 7. Marking scheme summary

Total 100 marks. Group 35 (Table 1), individual 65 (Table 2). Useful to know what matters most.

| Criterion | Marks | Top marks need |
|---|---|---|
| Service architecture and API design | 8 | API on IIS reachable by both clients (4), stable MongoDB connection used by all endpoints (4), all business logic in the API |
| Database design and data modelling | 4 | All four collections with every required field, sample data, consistent references |
| Client build and architecture compliance | 12 | Pure native Android with SQLite (6), web app as a UI layer only (6) |
| UI and experience design | 6 | Mobile interfaces (2), web with Bootstrap 5 or Tailwind (2), home page (1), all pages complete (1) |
| Documentation and deployment | 5 | Screenshots, diagrams, references, contributions, challenges, source code as text, reproducible hosting steps |
| Web app features and business rules | 18 | Login and roles (4), user management (4), node management (5), slot booking management (5), with the 7-day and 12-hour rules |
| Mobile authentication and account management | 9 | Role-based login (2), pending activation view in web (2), create account (3), modify (1), deactivate (1) |
| Reservation workflow and booking management | 9 | Create (3), update (2), cancel (2), summary page after each action (2), 12-hour rule on update and cancel |
| Booking views and operational dashboards | 10 | Current and pending bookings (2), history (2), filter (2), pending reservations (2), count of approved future reservations (2), all live from the API |
| Grid operator verification and map | 7 | Scan QR, verify on server, mark job done (2), nearby stations on the map from stored latitude and longitude (5) |
| Service integration, local persistence and device capabilities | 12 | Web to API (2), mobile to API (2), SQLite (3), Google Maps (3), QR scanning (2) |

## 8. Rules from the document that affect the code

| ID | Rule |
|---|---|
| DR-1 | Every .cs file has a comment header block. |
| DR-2 | Every method has an inline comment at its beginning, in every language. |
| DR-3 | Code that is not written by you is referenced in a comment that says where it came from. |
| DR-4 | Screenshots of the app are unique. |
| DR-5 | Report contents: screenshots of all UIs, high-level diagram, use case diagram, DFD, database design, source code pasted as text, references, Git link, individual contributions, challenges. |
| DR-6 | README has the Git repository link (with individual contributions) and a video link (YouTube or OneDrive) of no more than 5 minutes explaining how the app works. |
| DR-7 | The project is developed under Git with meaningful, descriptive commits. |
| DR-8 | The zip file name includes the IT number, for example IT15895623.zip. |

The document says code that does not include DR-1, DR-2 and DR-4 will not be marked. DR-1 to DR-4 and DR-7 are kept in this build. DR-5, DR-6 and DR-8 only matter if someone packages the project like the original assignment.

## 9. Open decisions

The document does not answer these. Leave an entry as "open" until the team fills it in. If a task depends on an open decision, ask the user.

| ID | Decision | Notes | Answer |
|---|---|---|---|
| D-1 | Android language, Java or Kotlin | The document only says pure native Android. | open |
| D-2 | Web UI choice | The document allows Bootstrap 5, Tailwind CSS or React.js. Pick one for all four members. | proposed by Member 1: React with Bootstrap 5 (react-bootstrap), TypeScript |
| D-3 | How clients prove who they are to the API (token, session, other) | The document says login verifies the user type. It says nothing about the method. | proposed by Member 1: JWT bearer token, see section 13 |
| D-4 | Android networking and JSON | Android SDK only (`HttpURLConnection`, `org.json`) or a library. The document says "no frameworks", so confirm what counts. | decided by Member 3: Android SDK only, `HttpURLConnection` and `org.json` |
| D-5 | QR library and QR content | Options found: zxing-android-embedded (scans, and its `BarcodeEncoder` can draw a QR code) and Google ML Kit barcode scanning (scanning). Both are libraries, not cross-platform frameworks, but confirm they are allowed. The document only says "secure transaction QR code". | decided by Member 3: zxing-android-embedded, for drawing and later scanning |
| D-6 | Who approves a reservation | The document says "once approved" but not who approves or how. | open |
| D-7 | Meaning of "nearby" on the map | All nodes, or only nodes within some distance of the phone. The marking scheme says plot from stored latitude and longitude and show details on selection. | open |
| D-8 | What "reference data" is stored in SQLite | The document says login details and reference data. | open |
| D-9 | Fields of each collection and how a reservation links to a station and a slot, and how slot availability is counted | See section 5 for what the document implies. | Energy Reservation decided by Member 3: id, prosumer NIC, station id, slot id, scheduled time (UTC), state (pending, approved, cancelled, done), QR data. Other collections still open. |
| D-10 | Which app a Grid Operator uses to cancel for a prosumer | The document says with the assistance of a grid operator. | open |
| D-11 | .NET version, MongoDB location (local install or hosted), API base URL | IIS needs the matching .NET Hosting Bundle. | proposed by Member 1: .NET 10, MongoDB on the local machine, database SolarMicrogrid |
| D-12 | Repository layout | Suggestion in section 10. | proposed by Member 1: one repo with api, web and android folders |
| D-13 | Member names and my member number | Needed for the file header comment and for picking the scope in section 11. | open |
| D-14 | Which roles can log in where | The document does not say. Proposed: the login carries a platform (web or mobile). Web allows Backoffice and Grid Operator. Mobile allows Prosumer and Grid Operator. Backoffice on mobile is blocked until the team decides. | proposed |
| D-15 | Prosumer activation | The document has a pending activation view but not the flow. Proposed: a prosumer who registers is pending, a Backoffice user activates them in the web app, and a pending prosumer cannot log in. | proposed |
| D-16 | Prosumer deactivation | Proposed: the mobile request only sets a flag, and a Backoffice user deactivates and reactivates from the web app. | proposed |
| D-17 | First Backoffice user | Nobody can create it from the app. Proposed: the API creates one at startup from configuration when no Backoffice user exists. | proposed |
| D-18 | Which web functions are administration and which are operational | The document names both groups but lists neither. Proposed matrix in section 13. This decides the role-based access marks. | proposed |
| D-19 | Where each role lands after login | The document says login redirects correctly (web) and routes each role to the correct home screen (mobile). Proposed: Backoffice and Grid Operator each get their own web home route, and prosumer and operator each get their own mobile home screen. | proposed |
| D-20 | NIC format check | The document does not say to validate the NIC. Sri Lankan NICs are 9 digits plus V or X (old style) or 12 digits (new style). Confirm before enforcing. | open |

## 10. Suggestions (not from the document)

### Words

The document uses hub, node, station and grid node for the same thing. Using one word in code avoids confusion.

| Thing | Word in code | Words on screen |
|---|---|---|
| Solar hub, microgrid node, grid node | `Station` (matches the SolarStationInfo collection) | "Microgrid node" |
| Battery storage slot, booking slot | `Slot` | "Slot" |
| Power trading reservation, booking | `Reservation` | "Reservation" |
| Solar prosumer | `Prosumer` | "Prosumer" |

### Model names

`UserDetail`, `SolarStation`, `EnergyBookingSlot`, `EnergyReservation`. Keep the collection names as the document writes them.

### Reservation states

`pending`, `approved`, `cancelled`, `done`. These come from the words used in the document. The path is pending to approved to done. A reservation can be cancelled while pending or approved, if BR-5 allows it.

### Time

Store all times in UTC. Check BR-4 and BR-5 against the reservation start time on the server.

### Repository

One Git repository with three folders: `api`, `web`, `android`. One link for the README, and the commit history shows each member's work.

### Build order

1. API skeleton, MongoDB connection, deployed once on IIS early so hosting problems show up early (S-1 to S-4).
2. Login and roles (W-1, W-2, M-2).
3. Prosumer registration and management (W-3, W-4, M-3, BR-1, BR-2).
4. Stations and slots (W-5, BR-3).
5. Reservations with the 7-day and 12-hour rules (W-6, M-4, BR-4, BR-5).
6. Web pages finished and styled (W-8).
7. Android dashboard, history and search (M-6, M-7, M-10).
8. Map (M-8).
9. QR display and operator scan (M-5, M-9, BR-7, BR-8).
10. Sample data, cleanup, README.

### API routes

A starting list, one line per endpoint. Change it as decisions land.

| Area | Route | Does | Requirement |
|---|---|---|---|
| Auth | `POST /api/auth/login` | Checks credentials and returns the user type | W-2, M-2 |
| Web users | `GET/POST/PUT /api/users` | Backoffice manages Backoffice and Grid Operator users | W-1 |
| Prosumers | `POST /api/prosumers/register` | Registers a prosumer from the mobile app | M-3 |
| Prosumers | `GET /api/prosumers`, `GET /api/prosumers/{nic}`, `PUT /api/prosumers/{nic}` | List, read and update | W-3, M-3 |
| Prosumers | `POST /api/prosumers/{nic}/deactivate-request` | Prosumer asks to be deactivated | M-3 |
| Prosumers | `GET /api/prosumers/pending` | Pending activations | W-4 |
| Prosumers | `POST /api/prosumers/{nic}/activate`, `/deactivate`, `/reactivate` | Backoffice changes the state | W-3, BR-2 |
| Stations | `GET/POST/PUT /api/stations`, `POST /api/stations/{id}/deactivate` | Manage nodes, deactivation checks BR-3 | W-5, BR-3 |
| Slots | `GET/POST/PUT/DELETE /api/stations/{id}/slots` | Manage slots of a station | W-5, W-7 |
| Reservations | `POST /api/reservations` | Create, checks BR-4 | W-6, M-4 |
| Reservations | `PUT /api/reservations/{id}` | Update, checks BR-5 | W-6, M-4 |
| Reservations | `POST /api/reservations/{id}/cancel` | Cancel, checks BR-5 | W-6, M-4, BR-9 |
| Reservations | `POST /api/reservations/{id}/approve` | Approve, depends on D-6 | M-5 |
| Reservations | `GET /api/reservations`, `GET /api/reservations/mine`, `GET /api/reservations/mine?search=` | Lists for staff and for the prosumer, with search | W-7, M-7 |
| Dashboard | `GET /api/reservations/summary` | Pending count and approved future count | M-6 |
| Operator | `POST /api/reservations/verify` | Verifies the scanned QR data | M-9, BR-8 |
| Operator | `POST /api/reservations/{id}/complete` | Marks the job done | M-9 |

### Facts checked while preparing this file

- ASP.NET Core on IIS needs the .NET Hosting Bundle, which installs the .NET runtime and the ASP.NET Core Module. If it was installed before IIS, run it again after IIS is installed.
- The MongoDB C# driver is the `MongoDB.Driver` package. One `MongoClient` is meant to be reused across the app. Collections are read with `GetCollection<T>` and can use plain C# classes.
- The Maps SDK for Android shows a map in a `SupportMapFragment` or `MapView` and you use an `OnMapReadyCallback` to get the `GoogleMap`. It needs an API key. Google recommends keeping the key out of version control and needs Google Play services on the device or emulator.
- zxing-android-embedded can scan barcodes and its `BarcodeEncoder` can draw a QR code bitmap. Google ML Kit can scan. Both are open questions under D-5.

## 11. Team split (proposed, confirm with the team)

Four members, each with a slice of web, mobile and API work. Table 2 is the individual mark (65 in total) and Table 1 is the shared group mark (35 in total).

| Member | Focus | Web | Mobile | API and database |
|---|---|---|---|---|
| 1 | Users and accounts | W-1, W-2, W-3, W-4 | M-2, M-3 | Auth, roles, prosumer rules, User's detail collection, IIS hosting |
| 2 | Nodes and map | W-5 | M-8 | Station and slot endpoints, BR-3, SolarStationInfo and EnergyBookingSlots, MongoDB connection |
| 3 | Reservations | W-6, web as UI layer | M-4, M-5 | Reservation endpoints, BR-4, BR-5, Energy Reservation collection |
| 4 | Views, operator, local storage | Home page, operator tools (W-7) | M-6, M-7, M-9, M-10, native Android with SQLite | Booking read endpoints, QR verify and complete, BR-8 |

## 12. Not in scope

Anything not written above is not in scope. That includes payments, real energy metering, push notifications, email, admin analytics, multi-language support and an iOS app. If a task needs one of these, ask first.

## 13. Auth and roles

### From the document

- There are two web roles, Backoffice and Grid Operator (W-1). Prosumers use the mobile app. Grid Operators use both the web app and the mobile app.
- Only Backoffice users have access to system administration functions. Grid Operators access operational tools.
- Web login verifies the user type and redirects correctly. Mobile login routes each role to the correct home screen (W-2, M-2).
- All business logic, including access checks, lives in the API. The clients only show screens.

The document does not name a login method or token type. It also does not list which web functions are administration and which are operational.

### Proposed contract (not in the document)

Yes, this is role-based access control (RBAC): three fixed roles, checked in the API. There are no per-user permissions and no policy rules beyond the role and the account status.

How it works:

1. The client sends user id, password and platform (`web` or `mobile`) to `POST /api/auth/login`.
2. The API checks the password hash, the account status and that the role may log in on that platform (D-14).
3. The API returns a signed JWT. It carries the user id and the role, and it expires after a set time.
4. The client keeps the token. The web app uses `localStorage`. The Android app keeps it in SQLite if D-8 says so.
5. Every later request sends the header `Authorization: Bearer <token>`.
6. The API validates the signature and expiry, then checks the role with `[Authorize(Roles = "...")]`.
7. Rules about "my own" data (a prosumer's reservations, profile) read the user id from the token. Never read it from the request body or the URL.
8. 401 means no valid token. 403 means a valid token but the role or the account status is not allowed.
9. The API also checks that the account is still `active` on every request, in one place (the JWT bearer `OnTokenValidated` event). Without this, a deactivated user's old token keeps working until it expires.

Role names in code: `Backoffice`, `GridOperator`, `Prosumer`.

Rules for every member:

- Put `[Authorize]` on every controller and open only login and prosumer registration with `[AllowAnonymous]`. Deny by default.
- Add `[Authorize(Roles = "...")]` to every action that needs a specific role.
- Never trust a role or user id sent by a client. The web and Android apps only hide menu items, the API is what blocks.

Proposed access matrix for D-18. Backoffice can do everything a Grid Operator can on the web, because the document only restricts the administration functions to Backoffice.

| Function | Backoffice | Grid Operator | Prosumer |
|---|---|---|---|
| Log in on web | yes | yes | no |
| Log in on mobile | no | yes | yes |
| Manage web users (W-1) | yes | no | no |
| Manage prosumers, pending activations, activate, deactivate, reactivate (W-3, W-4) | yes | no | no |
| Manage nodes, slots and schedules (W-5) | yes | no | no |
| Update battery slot availability (W-7) | yes | yes | no |
| Monitor bookings on web (W-7) | yes | yes | no |
| Create, update, cancel reservations on web, including for a prosumer (W-6, BR-9) | yes | yes | no |
| Register, edit own profile, request deactivation (M-3) | no | no | yes, own record only |
| Reserve, modify, cancel own reservations (M-4) | no | no | yes, own only |
| Dashboard, history, search, map (M-6, M-7, M-8) | no | no | yes |
| Scan and finalise a QR code (M-9) | no | yes | no |
| Approve a reservation (M-5) | open, see D-6 | open | no |
