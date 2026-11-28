# PRAYAS Campus Tour Platform

An operations platform for PRAYAS, Rural Development Centre (RDC), IIT Hyderabad, starting with its Campus Tour programme: schools and colleges bring students to the campus for a full-day visit to the library, labs, sports complex and other venues.

Today a visit is arranged over calls, messages and spreadsheets. This platform gives schools one place to request and manage a visit, and gives PRAYAS staff one place to review requests, enforce capacity rules and hand work to the right domain.

**Status:** Phases 1 and 2 are complete and running locally: requests, approvals, teams, tasks, itineraries and readiness tracking. The project is not deployed yet. See [Roadmap](#roadmap) for what is still to come.

## Screenshots

### For schools and colleges

A five-step request form that needs no login:

| 1. School and contact | 2. Visit details | 3. Venues | 4. Logistics |
| --- | --- | --- | --- |
| [![Step 1](docs/schools/step-1-school-contact.png)](docs/schools/step-1-school-contact.png) | [![Step 2](docs/schools/step-2-visit-details.png)](docs/schools/step-2-visit-details.png) | [![Step 3](docs/schools/step-3-venues.png)](docs/schools/step-3-venues.png) | [![Step 4](docs/schools/step-4-logistics.png)](docs/schools/step-4-logistics.png) |

| 5. Review before sending | Request sent, with a private tracking link |
| --- | --- |
| [![Review](docs/schools/step-5-review.png)](docs/schools/step-5-review.png) | [![Request sent](docs/schools/request-sent.png)](docs/schools/request-sent.png) |

**A repeat request is blocked** while the same school or contact number already has a pending or approved one:

![Repeat request blocked](docs/schools/repeat-request-blocked.png)

**Tracking link: a new date offered by staff.** When a rejected request is reopened with a new date, the school sees it here and the request reopens only once they accept:

![Date offered](docs/schools/date-offered.png)

**Tracking link: the plan for the day**, once staff have planned the stops:

![Plan for the day](docs/schools/school-plan.png)

### For staff

| Sign in | Coordinator view (read-only) |
| --- | --- |
| [![Staff sign-in](docs/staff/sign-in.png)](docs/staff/sign-in.png) | [![Coordinator dashboard](docs/staff/dashboard-coordinator.png)](docs/staff/dashboard-coordinator.png) |

**Lead and faculty in-charge dashboard**, with approve, reject and reopen:

![Lead dashboard](docs/staff/dashboard-lead.png)

**My tours**, where each coordinator or volunteer sees only their own shifts (shown here as a volunteer, using the "Test as" switcher):

![My tours](docs/staff/my-tours.png)

**Tour requests**, where approved tours also show how ready their itinerary is:

![Tour requests with readiness](docs/staff/tour-requests-readiness.png)

**Teams**, the chain of command: a domain head picks coordinators, and a coordinator picks volunteers, from their own domain (shown as the Hospitality & Logistics head):

![Teams](docs/staff/teams.png)

**Approvals**, with venue permissions and the two-step lunch approval (the mess step opens only once the hostel office has approved):

![Approvals](docs/staff/approvals.png)

**Itineraries**, with each stop marked Ready, Not ready or Blocked (a block needs a reason) and one readiness label for the tour:

![Itineraries](docs/staff/itineraries.png)

**Tasks**, where leads see every domain and everyone sees who has finished their part:

![Tasks](docs/staff/tasks-lead.png)

| My tasks: not done yet | My tasks: part done, with a note and proof link |
| --- | --- |
| [![My tasks](docs/staff/my-tasks.png)](docs/staff/my-tasks.png) | [![My tasks, done](docs/staff/my-tasks-done.png)](docs/staff/my-tasks-done.png) |

| Venues | Approval contacts | Staff and roles |
| --- | --- | --- |
| [![Venues](docs/staff/admin-venues.png)](docs/staff/admin-venues.png) | [![Approval contacts](docs/staff/admin-approval-contacts.png)](docs/staff/admin-approval-contacts.png) | [![Staff and roles](docs/staff/admin-staff-roles.png)](docs/staff/admin-staff-roles.png) |

## Features

### For schools and colleges (no login needed)

- A five-step request form: school and contact, visit details, venues, logistics, review and submit.
- School or college mode, which changes the questions asked (grades for schools; course and year for colleges).
- A private tracking link issued on submission, where the school can:
  - see the status of the request and any note from staff,
  - revise the headcount, grade by grade, until the freeze date,
  - change the lunch requirement,
  - ask for a different date and time, before or after approval (the date only changes once staff approve it),
  - cancel the request.

### For staff (Google sign-in, institute accounts only)

- A dashboard listing every request, with status and visit-date filters.
- An expandable detail view per request: address, contacts, teachers, grade breakdown, lunch, vehicle number.
- Approve, or reject with a reason that the school sees on its tracking page.
- Reopen a rejected request (Lead or Faculty in-charge only, reason required).
- Date-change requests from schools appear as a banner on the request; staff approve or decline (with a reason) and nothing moves until they do.
- Assign coordinators and volunteers to an approved tour, for the whole visit or part of it, choosing only from people who are free. Hand a shift over mid-tour to another person of the same kind.
- An **Approvals** page for Hospitality & Logistics and the Campus Tour team: every upcoming approved tour that still needs a venue approval or the two-step lunch approval (hostel office, then mess in-charge), with who to contact and buttons to record "asked", "approved" or "denied".
- A **Teams** page for the chain of command. For each upcoming approved tour, every domain that has a job on it (Hospitality & Logistics, Volunteer Coordination) shows its coordinators and volunteers. The faculty in-charge and leads can put anyone on any team; a domain head picks coordinators from their own domain; a coordinator picks volunteers from theirs. People see their own domain jobs on **My tasks**.
- A **Tasks** page showing where each domain's tasks for upcoming tours stand. The faculty in-charge and leads see every domain with totals; heads, coordinators and volunteers see their own domains, with the team on each task. Coordinators and heads can set the status of tasks that aren't worked out from approvals.
- An **Itineraries** page where the Campus Tour team plans each approved tour as a timed list of stops (a venue, or an activity like a welcome talk or lunch). Stops must fall inside the visit window and can't overlap, and a venue can't be booked by two tours at the same time. Schools see the plan on their tracking link.
- A **My tasks** page where each person on a domain's team sees their own jobs on upcoming tours and ticks off their part, with an optional note and an optional proof link (for example a photo in a shared folder). The Tasks page shows who has finished. The ticks are cleared if the tour moves to a new date.
- Readiness tracking on each itinerary stop (not ready, ready or blocked, with a reason when blocked). Each tour rolls up to one label that also shows on the Tour requests page, and a date change resets every stop.
- A **My tours** page where every staff member sees their own upcoming and finished shifts, with the school, headcount and contact details.
- Reopen a rejected request, optionally offering the school a new date. The school sees the date on their tracking link and the request reopens only when they accept it; if they decline, it stays rejected.
- Issue a fresh tracking link for a school that lost theirs.
- Installable as an app on phone or laptop (PWA).

### For leads and faculty in-charge

- Venue management: type, capacity, whether a visit needs approval, and who approves it.
- A directory of approval contacts (department heads, hostel office, mess in-charge).
- Staff and role management, with guards so an admin cannot lock themselves out.

## Engineering highlights

**A daily cap that holds under concurrent approvals.** Only a fixed number of tours can be approved per day (3 by default). Approval decisions for the same date are serialised with a PostgreSQL advisory lock keyed by the visit date, combined with optimistic locking on the entity. An integration test fires simultaneous approvals at a real Postgres database and checks the cap is never exceeded. That test caught a real bug: the status change was being flushed before the cap was counted, so a request counted itself.

**Tasks spawned across domains automatically.** PRAYAS has ten domains (Hospitality and Logistics, Volunteer Coordination, Transport and others). Approving a tour creates the tasks each domain owes it: lunch, volunteers, and venue approvals, the last only when a chosen venue needs one. This is built on a generic `Programme` and `Requirement` model, so future event types can reuse it.

**A public form protected without accounts.** Tracking tokens are stored only as SHA-256 hashes and shown once. The submit endpoint has per-IP rate limiting (Bucket4j) with a "try again at" time, a honeypot field, and a server-side CAPTCHA verification hook.

**Domain-scoped permissions.** Each person has a global role (Faculty in-charge, Lead or Member) plus a role within one domain (Head, Coordinator or Volunteer). Authorisation is checked on the server, and the interface shows only what the signed-in person can do.

**Business rules enforced on both ends.** Minimum notice before a visit, permitted visiting hours, minimum visit length, and a headcount freeze before the visit date are all configurable, validated in the browser for fast feedback and again on the server.

**Nobody can be double-booked, even by a race.** Shifts live in a `tour_assignment` table guarded by a PostgreSQL exclusion constraint (`EXCLUDE USING gist (user_id WITH =, tstzrange(starts_at, ends_at) WITH &&)`), so two people assigning the same volunteer at the same instant cannot both succeed. The service checks first to give a readable message, and turns a constraint violation into the same message. A hand-over shortens the old shift and starts the new one at exactly that moment; back-to-back shifts are allowed because the ranges are half-open. Tour times are plain campus-clock times, so shifts are anchored to `Asia/Kolkata` rather than the server's zone.

**Approval chains that open and close themselves.** Approving a tour creates one approval step for each chosen venue that needs a permission, plus a two-step lunch chain. The mess step stays locked until the hostel office has approved, and closes again, losing its recorded answer, if that approval is undone. The status of the matching task (Venue approvals, Lunch) is worked out from its steps, so the task list and the detailed steps never disagree. If a tour's date moves, every answered step goes back to the start, because permission for the old date doesn't carry over. Steps are created inside the approval's own transaction through an event, so a tour is never approved without them.

**Changes that need approval before they take effect.** A school's request for a new date is parked in separate `requested_*` columns, so the real date is untouched until staff approve. Approving a change on an already-approved tour takes the same per-date lock and daily-cap check as a normal approval, moves the requirement due dates, and publishes a `TourDateChangedEvent`; the assignment module listens for it and releases the shifts that were planned for the old date.

**Other details.** Versioned schema migrations with Flyway, an audit log of decisions, and consistent JSON error responses.

**A chain of command with the rules in one place.** Who may put whom on a domain's team for a tour is decided by one method in `DomainTeamService`, and the same method decides what the Teams page offers each person: someone who can't assign gets no buttons and no names to pick from, while the server refuses the request anyway if they try. A coordinator can only add volunteers to a tour they were themselves put on, and a head or coordinator can only choose people who belong to their own domain. A unit test covers each rule in both directions.

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Security (OAuth2 login), Spring Data JPA |
| Database | PostgreSQL 15, Flyway |
| Frontend | React, TypeScript, Vite, React Router, Axios |
| App install | vite-plugin-pwa (Workbox service worker) |
| Testing | JUnit and Mockito unit tests for the business rules, plus integration tests against a real PostgreSQL database |

## Project structure

```
prayas-backend/     Spring Boot REST API
prayas-frontend/    React app: public form, tracking page, staff dashboard, admin
```

## Running locally

You need Java 21, Maven, PostgreSQL 15 and Node.js (LTS).

### 1. Create the database

In `psql`, as the postgres superuser:

```sql
CREATE DATABASE prayas;
CREATE USER prayas WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE prayas TO prayas;
\c prayas
GRANT ALL ON SCHEMA public TO prayas;
```

### 2. Set environment variables

| Variable | Purpose |
| --- | --- |
| `DB_PASSWORD` | Database password. Defaults to `changeme` for local use. |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID, needed for staff sign-in. |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret. |

Create the Google OAuth client as a web application with the redirect URI `http://localhost:8080/login/oauth2/code/google`.

### 3. Start the backend

```
cd prayas-backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Flyway creates the tables on first start. The API runs at `http://localhost:8080`. To check it, open `http://localhost:8080/api/v1/public/venues`, which should return the seeded venues as JSON.

### 4. Start the frontend

```
cd prayas-frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173`:

| Path | Page |
| --- | --- |
| `/` | Public request form |
| `/status/<token>` | Tracking page for a submitted request |
| `/staff` | Staff dashboard |
| `/staff/my-tours` | The signed-in person's own shifts |
| `/staff/my-tasks` | The signed-in person's own jobs, to tick off |
| `/staff/tasks` | Where each domain's tasks stand |
| `/staff/approvals` | Venue and lunch approvals |
| `/staff/teams` | Coordinators and volunteers for each tour |
| `/staff/itineraries` | Plan and readiness for each tour |
| `/staff/admin` | Admin page (Lead and Faculty in-charge) |

Staff sign-in works only for accounts that exist in the `app_user` table. Create the first Lead account directly in the database, then add everyone else from the admin page.

### Running the tests

The tests use a separate database named `prayas_test`, created the same way as `prayas`.

```
cd prayas-backend
mvn test
```

## Roadmap

Planned next: photo check-ins by volunteers during a tour, uploading proof as a file instead of a link, and deployment.

## Known limitations

These are open items to close before deployment:

- CSRF protection is currently switched off for the API during local development and needs to be restored for the browser-based staff pages.
- The CAPTCHA widget is not wired into the public form yet; the local profile uses a stub verifier.
- Lead and Faculty in-charge currently have identical access.
- Tours approved before the approval steps existed have none. Only tours approved from now on get venue and lunch steps.
- The two lunch steps (hostel office, then mess in-charge) have no contact preset; only venue steps start with the venue's approver from the authority directory.
- Nothing marks a tour as completed yet. Shifts for tours whose date has passed show under "Finished" on My tours, and assigning people to a past tour is refused, but the request itself stays Approved.
- Moving an approved tour to a new date releases everyone assigned to it, so the tour has to be staffed again. Shifts are not carried over to the new date.
- A school's new date must be later than the current one and respect the minimum notice period, even for an approved tour.
