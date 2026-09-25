# Feature Specification: Business Trip Management

**Feature Branch**: `001-business-trip-management`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Build a Web-App, where a user can manage his business trips. The user can choose from 3 predefined cars. When the user select a car, he is ask to enter a start date and time, a destination and a end time of his trip. The so defined business trip is stored. A user can see his bussiness trips in a list view sorted by start date."

## Clarifications

### Session 2026-09-27

- Q: Should users be allowed to edit or delete trips that have already started or ended? → A: No. Only trips that have not started yet can be edited or deleted; trips that have started or ended are read-only.
- Q: Before picking a car, should users be able to see which cars are already booked for the time they want? → A: Yes. The user enters start and end first; all three cars are then shown, booked cars are marked "not available" and cannot be selected; then the destination is entered and the trip saved.
- Q: Besides employees recording their own trips, does anyone else need a special role, such as a fleet manager who can see or manage all bookings? → A: No. There is a single role (employee) in this version; nobody can see other users' trip details.
- Q: Should users be able to record a trip whose start date and time is already in the past? → A: No. The start must lie in the future; a past start is rejected with an explanation.
- Q: How long after a trip ends should it be kept before the system deletes it automatically? → A: Decide with the data protection officer (DPO) later; the system must support automatic deletion after a configurable retention period.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a business trip with a pool car (Priority: P1)

An employee who needs to travel for work opens the web app and first enters when the trip
starts and ends (date and time). The app then shows the three pool cars and marks those
already booked for that period as "not available". The employee picks a free car, enters the
destination, saves the trip, and receives confirmation that it has been recorded.

**Why this priority**: Recording a trip is the core purpose of the application. Without it,
there is nothing to list or manage.

**Independent Test**: Can be fully tested by entering a period, selecting a free car, entering
the destination,
saving, and confirming that the system reports the trip as stored and it can be retrieved
again after reloading the application.

**Acceptance Scenarios**:

1. **Given** a signed-in user starts recording a trip, **When** the first step is shown,
   **Then** they are asked for start date and time and end date and time.
2. **Given** a user has entered a valid period, **When** they continue, **Then** exactly the
   three predefined cars are shown, each identifiable by name and license plate, and each
   marked as available or "not available" for that period.
3. **Given** a car is marked "not available", **When** the user tries to select it, **Then**
   it cannot be selected, and assistive technologies announce it as unavailable.
4. **Given** all three cars are booked for the entered period, **When** the car selection is
   shown, **Then** a message explains that no car is free for that period and invites the
   user to change the start or end.
5. **Given** a user has selected a free car, **When** the next step is shown, **Then** they
   are asked for the destination, and the chosen period and car are shown for review.
6. **Given** a user has entered a valid period, selected a free car, and entered a
   destination, **When** they save, **Then** the trip is stored and a confirmation message is
   shown.
7. **Given** a user has left a required field empty, **When** they try to save, **Then** the
   trip is not stored and a message names the missing field and how to fix it.
8. **Given** a user has entered an end date/time that is not after the start date/time,
   **When** they try to continue, **Then** the trip is not stored and a message explains that the
   end must be after the start.
9. **Given** a user has entered a start date/time that is not in the future, **When** they
   try to continue or save, **Then** the trip is not stored and a message explains that
   trips can only be booked with a future start.

---

### User Story 2 - View my business trips in a list (Priority: P2)

A user wants an overview of the trips they have recorded. They open the list view and see all
of their own trips, ordered by start date, with the key details of each trip visible at a
glance.

**Why this priority**: The list gives the recorded data its value to the user (planning,
reference, proof of travel), but it depends on trips existing first.

**Independent Test**: Can be tested by seeding a user with several trips with different start
dates and verifying that the list shows exactly that user's trips in ascending start-date
order with car, start, destination, and end displayed.

**Acceptance Scenarios**:

1. **Given** a user has recorded several trips, **When** they open the list view, **Then**
   all of their trips are shown sorted by start date and time, earliest first.
2. **Given** each trip in the list, **When** the user looks at an entry, **Then** they see the
   car, start date and time, destination, and end date and time.
3. **Given** two different users have each recorded trips, **When** one user opens the list
   view, **Then** only their own trips are shown.
4. **Given** a user has not recorded any trips, **When** they open the list view, **Then** a
   friendly message explains that no trips exist yet and offers a way to create one.
5. **Given** a user has just saved a new trip, **When** they open the list view, **Then** the
   new trip appears in its correct position according to its start date.

---

### User Story 3 - Change or remove a recorded trip (Priority: P3)

A user notices that an upcoming trip has the wrong details or is no longer needed. From the
list view they open the trip (only trips that have not started yet can be changed), change any of its details (car, start, destination, end), and
save the changes; or they delete the trip after confirming the deletion.

**Why this priority**: Correcting and removing trips keeps the data accurate and frees cars
for other users, and it lets users delete their personal data (GDPR/DSGVO). It builds on
Stories 1 and 2.

**Independent Test**: Can be tested by seeding a user with a trip, editing its details and
verifying the list reflects the change (including re-sorting), then deleting it and verifying
it no longer appears and its car time slot is free again.

**Acceptance Scenarios**:

1. **Given** a user has a recorded trip that has not started yet, **When** they choose to edit
   it, **Then** a form pre-filled with the trip's current start, end, car, and destination is
   shown, following the same steps as recording a trip; the trip's own car counts as
   available for its current period.
2. **Given** a user edits a trip with valid values, **When** they save, **Then** the changes
   are stored, a confirmation is shown, and the list shows the trip in its correct position.
3. **Given** a user edits a trip with invalid values (missing field, end not after start,
   car already booked for an overlapping time by another trip), **When** they save, **Then**
   the changes are not stored and the same messages as when creating a trip are shown.
4. **Given** a user chooses to delete a trip, **When** they are asked to confirm and confirm,
   **Then** the trip is permanently removed and no longer appears in the list.
5. **Given** a user chooses to delete a trip, **When** they cancel the confirmation, **Then**
   the trip remains unchanged.
6. **Given** a user's trip has already started or ended, **When** they view it in the list,
   **Then** no edit or delete option is offered, and any attempt to change or delete it
   (e.g., by manipulating a request) is refused with a message that past and ongoing trips
   cannot be changed.
7. **Given** a trip belongs to another user, **When** a user tries to edit or delete it (e.g.,
   by manipulating a request), **Then** the action is refused and the trip is unchanged.

---

### User Story 4 - Prevent double booking of a car (Priority: P1)

The three cars are a shared pool. When a user records or changes a trip, the system ensures
the selected car is not already booked by anyone for an overlapping period, so no two
employees plan to use the same car at the same time.

**Why this priority**: Without conflict prevention the recorded trips cannot be relied on to
actually get a car, which undermines the core purpose of Story 1.

**Independent Test**: Can be tested by seeding a trip for car A from 09:00 to 12:00 by user X,
then having user Y attempt trips for car A at 11:00–13:00 (rejected), 12:00–14:00 (accepted),
and for car B at 10:00–11:00 (accepted).

**Acceptance Scenarios**:

1. **Given** car A is booked 09:00–12:00 on a day, **When** any user enters a period that
   overlaps it, **Then** car A is shown as "not available" and cannot be selected.
2. **Given** car A was free when the user selected it but another user booked it for an
   overlapping period before the user saved, **When** the user saves, **Then** the trip is
   not stored, the user returns to the car selection with updated availability, and a message explains
   that the car is already booked for that time and suggests choosing another car or time.
3. **Given** car A is booked 09:00–12:00, **When** a user enters a period starting exactly at
   12:00, **Then** car A is shown as available and the trip can be stored (back-to-back trips are allowed).
4. **Given** car A is booked for an overlapping period, **When** a user saves a trip for car
   B in the same period, **Then** the trip is stored.
5. **Given** a user edits their own trip without changing it into a conflict with another
   trip, **When** they save, **Then** the trip does not conflict with itself.
6. **Given** a car is shown as "not available" or a conflict message is shown, **When** the user reads it, **Then** it does not
   reveal who booked the car or where they are going.
7. **Given** two users try to book the same car for overlapping times at the same moment,
   **When** both save, **Then** exactly one trip is stored and the other user receives the
   conflict message.

---

### Edge Cases

- A car is booked for the entered period: it is shown as "not available" and cannot be
  selected (see User Story 4); a trip ending exactly when another starts is not an overlap.
- A user goes back and changes the start or end after selecting a car: availability is
  re-evaluated; if the selected car is no longer free, the selection is cleared and the user
  is told why.
- A user deletes a trip: the car's time slot becomes available to all users immediately.
- A trip's start time passes while the user has its edit form open: saving is refused with a
  message that the trip has already started and can no longer be changed.
- A user edits an upcoming trip and moves its start into the past: the change is refused
  (FR-006a).
- A trip is edited or deleted by its owner while another user views a conflict message for
  the same slot: the other user can retry and succeeds if the slot is now free.
- A user enters a start date/time in the past: the period is rejected with a message that
  trips can only be booked for a future start, and the user is asked to correct it.
- The start time passes while the user is still filling in the form: saving is refused with
  the same message; the entered data is kept so the user can adjust the start.
- A trip spans multiple days (e.g., starts Monday 08:00, ends Wednesday 18:00): accepted, as
  end date and time are entered separately from the start.
- A destination is entered with only whitespace: treated as empty and rejected.
- A destination is extremely long: input is limited to 200 characters and the user is told
  about the limit.
- Two trips of the same user have the identical start date/time: both are shown; ties are
  ordered by the time the trip was recorded.
- Saving fails because of a technical problem: the user is told that the trip was not saved,
  their entered data is kept in the form, and they can retry.
- The user abandons the form before saving: nothing is stored.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST require users to sign in before they can record or view trips.
- **FR-001a**: System MUST provide a single user role (employee) with identical permissions
  for all users; there MUST be no role that can view, edit, or delete other users' trips.
- **FR-002**: System MUST offer exactly three predefined cars for selection; users MUST NOT be
  able to add, change, or remove cars.
- **FR-003**: System MUST display each car with a human-readable name and license plate so
  users can tell the cars apart.
- **FR-004**: When recording or editing a trip, the system MUST ask first for start date and
  time and end date and time, then show the car selection, then ask for the destination.
- **FR-004a**: The car selection MUST show all three cars for the entered period and mark cars
  booked for an overlapping period as "not available"; unavailable cars MUST NOT be
  selectable. If no car is free, the system MUST say so and invite the user to change the
  period.
- **FR-005**: System MUST treat car, start date and time, destination, and end date and time
  as required and MUST reject a trip missing any of them.
- **FR-006**: System MUST reject a trip whose end date and time is not after its start date
  and time.
- **FR-006a**: System MUST reject a new or edited trip whose start date and time is not in the
  future, based on the server's current time.
- **FR-007**: System MUST reject a destination that is empty, whitespace-only, or longer than
  200 characters.
- **FR-008**: System MUST validate all input on the server side, independently of any checks
  performed in the user's browser.
- **FR-009**: System MUST durably store each valid trip and associate it with the user who
  recorded it.
- **FR-010**: System MUST confirm successful storage to the user, and MUST clearly tell the
  user when a trip could not be stored, stating what went wrong and what to do next.
- **FR-011**: System MUST provide a list view showing the signed-in user's trips sorted by
  start date and time in ascending order (earliest first).
- **FR-012**: Each list entry MUST show the car, start date and time, destination, and end
  date and time.
- **FR-013**: System MUST show a user only their own trips; a user MUST NOT be able to view
  or access trips of other users.
- **FR-014**: System MUST show an informative empty state when the user has no trips.
- **FR-015**: System MUST display dates and times in the German format (e.g., 27.09.2026,
  14:30) in the local time zone (Europe/Berlin).
- **FR-016**: All screens MUST meet WCAG 2.1 level AA / BITV 2.0, including full keyboard
  operability and screen-reader-accessible form labels and error messages.
- **FR-017**: System MUST reject any new or changed trip whose time period overlaps an
  existing trip for the same car, regardless of which user recorded it. Periods overlap when
  one starts before the other ends; a trip starting exactly when another ends does not
  overlap.
- **FR-018**: System MUST enforce FR-017 even when several users save at the same moment, so
  that no car is ever double-booked.
- **FR-019**: Availability indicators and conflict messages MUST NOT reveal the owner or
  destination of other trips. A conflict message (e.g., when a car was booked by someone else
  between selection and saving) MUST state that the car is booked for that period and suggest
  another car or time.
- **FR-020**: Users MUST be able to edit all details (car, start, destination, end) of their
  own trips that have not started yet; the edited start MUST still lie in the future (FR-006a); edited
  trips MUST pass the same validation as new trips (FR-005 to FR-008, FR-017), ignoring the
  trip being edited when checking conflicts.
- **FR-021**: Users MUST be able to delete their own trips that have not started yet, after an
  explicit confirmation; deletion MUST remove the trip permanently and free the car's time slot.
- **FR-022**: System MUST refuse edit and delete requests for trips not owned by the
  requesting user.
- **FR-023**: System MUST treat trips whose start date and time has been reached (ongoing or
  past trips) as read-only and MUST refuse any edit or delete request for them, based on the
  server's current time.
- **FR-024**: System MUST automatically and permanently delete trips once a configurable
  retention period after their end date and time has passed; the period's value is set by
  operators without changing the software (value to be defined with the DPO before go-live).

### Key Entities

- **Car**: One of three predefined pool vehicles. Attributes: display name, license plate.
  Fixed set; not editable by users.
- **Business Trip**: A single trip recorded by a user. Attributes: selected car, start date
  and time, destination (free text, max. 200 characters), end date and time, recording user,
  time of recording. Belongs to exactly one user and references exactly one car.
- **User**: An employee who records trips; all users have the same role and permissions. Identified through the organization's sign-in.
  Owns zero or more business trips.

### Data Protection & Security

- **Data processed**: user identity (name/user ID), trip destinations, and trip times. Together
  these reveal a person's whereabouts and are personal data under GDPR (DSGVO).
- **Protection needs**: confidentiality (trips visible only to their owner), integrity (trips
  cannot be altered by other users; started trips cannot be altered at all), availability during working hours.
- **Relevant threats**: access to, modification, or deletion of other users' trips by
  manipulating requests (broken access control), inferring other users' whereabouts from
  availability indicators or conflict messages (only "available/not available" per car and
  period is disclosed), selecting an unavailable car by manipulating requests, injection via the destination field, session hijacking.
- **Deletion**: users can delete their own upcoming trips at any time. Trips that have started
  are kept as a record of car use and are deleted automatically when the retention period
  expires (FR-024); erasure requests for them are handled by the organization's data protection
  process.
- **Data minimization**: only the data listed above is collected; no mileage, cost, or
  passenger data in this version.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can record a complete business trip (enter period, select a
  free car, enter destination, save) in under 2 minutes without assistance.
- **SC-002**: At least 90% of users in a usability test successfully record a trip on their
  first attempt.
- **SC-003**: The list view of a user with up to 500 trips appears in under 2 seconds.
- **SC-004**: 100% of saved trips appear in the user's list in the correct start-date order.
- **SC-005**: 0 cases in which a user can see or access another user's trips (verified by
  access-control tests).
- **SC-006**: All screens pass an accessibility audit against WCAG 2.1 AA / BITV 2.0 with no
  critical findings.
- **SC-007**: 0 double bookings: no car ever has two stored trips with overlapping periods,
  including under simultaneous saving (verified by concurrency tests).
- **SC-008**: A user can correct or delete an upcoming trip in under 1 minute.

## Assumptions

- Users are employees of a single organization and sign in through the organization's existing
  sign-in mechanism; no self-registration is needed.
- The three cars (names and license plates) are configured by operators before go-live; their
  concrete values will be provided by the business owner.
- "End time" is interpreted as end date and time, so trips spanning several days are possible.
- The list shows all of a user's trips (past and future) in one list; filtering, searching, and
  pagination are out of scope for this version.
- Approval workflows, cost reimbursement, mileage tracking, and export are out of scope.
- Administrative or fleet-manager functions (viewing all bookings, blocking cars for
  maintenance, cancelling other users' trips) are out of scope for this version and may be
  specified as a separate feature.
- The user interface is in German.
- The concrete retention period for trips (FR-024) is not yet decided and will be agreed with
  the data protection officer before go-live; this is a release blocker for production use,
  not for planning or development.
