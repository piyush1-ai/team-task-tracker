# Team Task Tracker API

A REST API for managing projects and tasks in a small team. Registered users can create projects, add other registered users as members, create and assign tasks, update task status, and view filtered, sorted, paginated task lists and task summaries.

This is the solution for the **Backend Engineering Internship Assignment** (Axionix). There is no frontend; the API is documented and tested through Swagger UI.

## Tech stack

| Area | Choice |
|---|---|
| Language / framework | Java, Spring Boot |
| Persistence | Spring Data JPA (Hibernate) |
| Database | MySQL 8 |
| Migrations | Flyway |
| Security | Spring Security, JWT (jjwt), BCrypt password hashing |
| Validation | Jakarta Bean Validation |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Spring MockMvc, H2 (in-memory, tests only) |
| Build | Maven (wrapper included) |

## Prerequisites

- JDK 17 or newer
- MySQL 8 running locally
- Git
- No separate Maven install is needed; the project includes the Maven wrapper (`mvnw`, `mvnw.cmd`)

## Setup and run

### 1. Clone the repository

```bash
git clone https://github.com/piyush1-ai/team-task-tracker.git
cd team-task-tracker
```

### 2. Database

The application creates the `task_tracker` database automatically on first start (`createDatabaseIfNotExist=true` in the URL below), so the MySQL user needs the `CREATE` privilege. If you prefer to create it yourself:

```sql
CREATE DATABASE task_tracker;
```

The tables are created by Flyway migrations on startup; see [Database and migrations](#database-and-migrations).

### 3. Environment variables

The application reads its configuration from environment variables. Spring Boot does **not** read a `.env` file by itself; `.env.example` is only a template that lists the variable names.

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | JDBC URL of the MySQL database | `jdbc:mysql://localhost:3306/task_tracker?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `your_mysql_password` |
| `JWT_SECRET` | Secret used to sign tokens. Must be at least 32 characters. | any long random string |
| `JWT_EXPIRATION` | Token lifetime in milliseconds | `3600000` (1 hour) |

Set them in the same terminal you use to start the app.

**Windows (PowerShell):**

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/task_tracker?createDatabaseIfNotExist=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace-with-a-long-random-string-of-at-least-32-characters"
$env:JWT_EXPIRATION="3600000"
```

**macOS / Linux / Git Bash:**

```bash
export DB_URL="jdbc:mysql://localhost:3306/task_tracker?createDatabaseIfNotExist=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="replace-with-a-long-random-string-of-at-least-32-characters"
export JWT_EXPIRATION="3600000"
```

If you run the project from an IDE, put the same five variables in the run configuration instead.

### 4. Start the application

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. On startup you should see Flyway apply the migrations, followed by the Spring Boot startup banner finishing.

## Database and migrations

Schema changes are managed with Flyway. The SQL files live in `src/main/resources/db/migration` and run automatically, in version order, when the application starts. Hibernate is set to `ddl-auto=validate`, so it only checks that the entities match the tables and never changes the schema.

| Table | Purpose |
|---|---|
| `users` | Registered users (email stored lowercase and unique, BCrypt password hash) |
| `projects` | Projects, with an `owner_id` |
| `project_members` | Join table between projects and users |
| `tasks` | Tasks, with project, assignee (optional), creator, and `created_at`, `updated_at`, `completed_at` timestamps |
| `flyway_schema_history` | Flyway's own bookkeeping table |

Deleting a project removes its tasks and memberships through `ON DELETE CASCADE`.

To reset the database during development, drop it and restart the application; Flyway rebuilds everything:

```sql
DROP DATABASE task_tracker;
```

Never edit a migration that has already been applied. Add a new versioned file instead.

## Running the tests

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

The suite has 16 integration tests. They use MockMvc against the full Spring context (security, controllers, services, JPA, error handler). They run on an in-memory H2 database in MySQL mode, configured in `src/test/resources/application-test.properties`, and Flyway applies the same migrations to it. **No MySQL and no environment variables are needed to run the tests.** Each test runs in a transaction that is rolled back, so tests are independent and repeatable.

| Area | What is covered |
|---|---|
| Authentication | register returns no password field; duplicate email (case-insensitive) rejected; successful login returns a token; failed login returns 401; missing token returns 401 |
| Project authorization | non-member cannot view a project; member can view after being added; member cannot add members; only the owner can delete a project |
| Tasks | invalid assignee (not a project member) rejected; valid assignee accepted; `completed_at` set on DONE and cleared on reopen; filtering by status and priority; page size above 50 rejected; only owner or creator can delete a task; summary counts |

## API documentation

Swagger UI is the primary API documentation:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

To call protected endpoints from Swagger UI:

1. Call `POST /auth/register`, then `POST /auth/login`.
2. Copy the `token` value from the login response (without quotes).
3. Click **Authorize**, paste the token, and confirm. Do not type the word `Bearer`.

### Endpoints

All endpoints except `/auth/*` require the header `Authorization: Bearer <token>`.

| Method | Endpoint | Purpose | Who may call it | Success |
|---|---|---|---|---|
| POST | `/auth/register` | Create a user | anyone | 201 |
| POST | `/auth/login` | Authenticate, returns a JWT | anyone | 200 |
| POST | `/projects` | Create a project (creator becomes owner and member) | authenticated user | 201 |
| GET | `/projects` | List projects the user is a member of | authenticated user | 200 |
| GET | `/projects/{projectId}` | View one project with its members | project members | 200 |
| POST | `/projects/{projectId}/members` | Add a registered user by email | project owner | 200 |
| DELETE | `/projects/{projectId}` | Delete the project, its tasks and memberships | project owner | 204 |
| POST | `/projects/{projectId}/tasks` | Create a task | project members | 201 |
| GET | `/projects/{projectId}/tasks` | Paginated, filtered, sorted task list | project members | 200 |
| PATCH | `/tasks/{taskId}` | Partially update a task | project members | 200 |
| DELETE | `/tasks/{taskId}` | Delete a task | project owner or task creator | 204 |
| GET | `/projects/{projectId}/summary` | Total tasks and counts by status and priority | project members | 200 |

### Task list query parameters

`GET /projects/{projectId}/tasks`

| Parameter | Description | Default |
|---|---|---|
| `status` | `TODO`, `IN_PROGRESS` or `DONE` | no filter |
| `priority` | `LOW`, `MEDIUM` or `HIGH` | no filter |
| `assigneeId` | Id of the assigned user | no filter |
| `dueFrom` | Due date on or after, e.g. `2027-01-01T00:00:00` | no filter |
| `dueTo` | Due date on or before, e.g. `2027-06-30T23:59:59` | no filter |
| `page` | Zero-based page number | `0` |
| `size` | Page size, from 1 to 50 | `10` |
| `sortBy` | `created_at` or `due_date` | `created_at` |
| `direction` | `asc` or `desc` | `desc` |

The response contains `content`, `page`, `size`, `totalElements` and `totalPages`.

### Example requests (curl)

These work in macOS, Linux and Git Bash. On Windows PowerShell, use Swagger UI or `curl.exe` with escaped quotes.

```bash
BASE=http://localhost:8080

# Register and log in
curl -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Piyush","email":"piyush@example.com","password":"password123"}'

curl -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"piyush@example.com","password":"password123"}'
# -> {"token":"eyJ..."}

TOKEN=<paste the token here>

# Create a project
curl -X POST $BASE/projects -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Alpha","description":"First project"}'

# Add a member (the user must already be registered)
curl -X POST $BASE/projects/1/members -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"email":"bob@example.com"}'

# Create a task
curl -X POST $BASE/projects/1/tasks -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Design database","priority":"HIGH","dueDate":"2027-01-15T18:00:00","assigneeId":2}'

# Mark it done (completed_at is set)
curl -X PATCH $BASE/tasks/1 -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"DONE"}'

# Filter, sort and paginate
curl "$BASE/projects/1/tasks?status=DONE&priority=HIGH&sortBy=due_date&direction=asc&page=0&size=5" \
  -H "Authorization: Bearer $TOKEN"

# Summary
curl $BASE/projects/1/summary -H "Authorization: Bearer $TOKEN"
```

The summary response looks like this (groups with no tasks are shown as 0):

```json
{
  "total": 4,
  "byStatus":   { "TODO": 2, "IN_PROGRESS": 1, "DONE": 1 },
  "byPriority": { "LOW": 1, "MEDIUM": 1, "HIGH": 2 }
}
```

## Error format

Every error uses the same JSON shape:

```json
{ "error": { "code": "EMAIL_ALREADY_EXISTS", "message": "Email already registered" } }
```

The `code` is stable and meant for clients; the `message` is for people.

| Case | Status | Codes |
|---|---|---|
| Invalid input | 400 | `VALIDATION_ERROR`, `INVALID_REQUEST_BODY`, `INVALID_PARAMETER`, `INVALID_ASSIGNEE`, `INVALID_DUE_DATE`, `INVALID_PAGINATION`, `INVALID_SORT`, `INVALID_DATE_RANGE` |
| Missing or invalid token | 401 | `UNAUTHORIZED` |
| Invalid credentials | 401 | `INVALID_CREDENTIALS` |
| Forbidden action | 403 | `NOT_A_MEMBER`, `NOT_PROJECT_OWNER`, `DELETE_NOT_ALLOWED` |
| Resource not found | 404 | `PROJECT_NOT_FOUND`, `TASK_NOT_FOUND`, `USER_NOT_FOUND`, `NOT_FOUND` (unknown URL) |
| Wrong HTTP method | 405 | `METHOD_NOT_ALLOWED` |
| Duplicate email, already a member | 409 | `EMAIL_ALREADY_EXISTS`, `ALREADY_A_MEMBER` |
| Unexpected server error | 500 | `INTERNAL_ERROR` (details are logged on the server, never returned) |

## Project structure

Code is layered: controllers handle HTTP only, services hold the business rules and authorization checks, and repositories handle data access.

```
src/main/java/com/axionix/team_task_tracker
 ├── configuration   OpenAPI / Swagger setup
 ├── controller      REST controllers (auth, projects, tasks)
 ├── dto             request and response records (no entity is returned directly)
 ├── entity          JPA entities and enums (User, Project, Task, Priority, Status)
 ├── exception       ApiException, ApiError and the global exception handler
 ├── repository      Spring Data repositories and task query specifications
 ├── security        JWT service, JWT filter, security configuration, 401 entry point
 └── service         business logic and authorization rules

src/main/resources
 ├── application.properties        all secrets come from environment variables
 └── db/migration                  Flyway migrations

src/test
 ├── java/com/axionix/team_task_tracker   integration tests
 └── resources/application-test.properties
```

## Assumptions and design decisions

**Authentication**
- Emails are trimmed and stored in lowercase, so uniqueness is case-insensitive.
- Passwords must be at least 8 characters and are stored as BCrypt hashes. A password or hash is never returned by any endpoint.
- Login returns the same "invalid email or password" error for an unknown email and a wrong password, so the API does not reveal which emails are registered.
- The token carries the user's email as its subject and expires after `JWT_EXPIRATION` milliseconds.

**Projects and membership**
- A user who is not a member of a project gets **403** (not 404) when viewing or modifying it.
- Adding an unregistered email returns 404, and adding someone who is already a member returns 409.
- `GET /projects` returns every project where the user is a member, not only owned ones, and is not paginated.

**Tasks**
- **Past due dates are rejected** with 400. On update the check applies only when a new `dueDate` is sent, so changing the status of an overdue task still works.
- **Due date is required** when creating a task; the assignment does not mark it optional, unlike the description and assignee.
- Due dates are ISO-8601 date-times without a timezone (for example `2027-01-15T18:00:00`) and are interpreted in the server's local time.
- An assignee who is not a project member is rejected with 400 (`INVALID_ASSIGNEE`), on create and on update.
- `completed_at` is set when a task's status changes to `DONE` and cleared when it moves away from `DONE`.
- PATCH treats a missing field as "leave unchanged".
- Only the project owner or the task's creator can delete a task; any member can update it.

**Listing**
- Invalid `page`, `size`, `sortBy` or `direction` values are rejected with 400 instead of being silently corrected.
- Sorting accepts only `created_at` and `due_date`, and ties are broken by task id so pages stay stable.
- The due date range filter is inclusive at both ends, and `dueFrom` after `dueTo` returns 400.

## Known limitations

- PATCH cannot clear an assignee or a due date, because a missing value means "unchanged".
- There are no refresh tokens, token revocation or rate limiting. A token stays valid until it expires.
- There are no user-management endpoints (list users, remove a member, change a password, delete a user).
- The summary endpoint runs several small count queries instead of one grouped query.
- Tests run on H2 in MySQL mode, which is not identical to MySQL; a real MySQL (for example through Testcontainers) would be closer to production.
- Timestamps have no timezone and use the server's local time.
- No caching or query tuning beyond the keys and foreign keys defined in the migrations.

## Bonus items

None of the optional bonus items (Docker Compose, refresh tokens, audit log, CI, deployment) are implemented.

## AI usage

AI tools were used while building this project. See [AI_USAGE.md](AI_USAGE.md) for the tools, example prompts, what was generated, what was verified or changed manually, and an example of an incorrect AI suggestion.
