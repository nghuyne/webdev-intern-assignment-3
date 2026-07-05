# G-Scores

A full-stack web application for importing, searching, and analyzing Vietnam's **THPT 2024** (national high-school graduation exam) results — built for the Golden Owl web developer internship assignment.

The dataset contains **1,061,605 candidates** across **9 subjects**. The app ingests the raw CSV into a normalized PostgreSQL schema, then exposes score lookup, score-distribution reporting, and a Group A leaderboard on top of it.

**Live demo:** [TODO: add Vercel URL here] — seeded with a **10,000-row sample** of the dataset (free-tier Postgres/Redis can't fit the full 1,061,605 rows). All features work identically at this scale; run locally via Docker Compose to see it with the full dataset. See [Deployment](#deployment) for how the demo is set up.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture & Key Design Decisions](#architecture--key-design-decisions)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Option A — Docker Compose (recommended)](#option-a--docker-compose-recommended)
  - [Option B — Run locally without Docker](#option-b--run-locally-without-docker)
- [Environment Variables](#environment-variables)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Deployment](#deployment)

---

## Features

### Must-have

| # | Requirement | Implementation |
|---|---|---|
| 1 | Import raw CSV into the database | Flyway migrations (schema + reference data) + a resumable, batch-based CSV seeder service — all versioned in this repo, no manual DB steps required |
| 2 | Look up a candidate's scores by registration number (SBD) | `GET /api/scores/{sbd}` with strict 8-digit input validation |
| 3 | Score report across 4 bands: `>= 8`, `[6, 8)`, `[4, 6)`, `< 4` | `GET /api/report/band-counts`, one row per subject |
| 4 | Chart of the 4 bands per subject | Interactive bar chart on the **Report** page (Recharts) |
| 5 | Top 10 Group A leaderboard (Toán, Vật lí, Hóa học) | `GET /api/leaderboard/group-a`, backed by a Redis sorted set with a verified Postgres fallback |

### Nice-to-have

| # | Requirement | Implementation |
|---|---|---|
| 1 | Responsive design | Tailwind CSS layout with a collapsible mobile drawer sidebar; verified on desktop, tablet, and mobile breakpoints |
| 2 | Docker | Multi-stage Dockerfiles for both services + a single `docker-compose.yml` that wires Postgres, Redis, backend, and frontend together with health checks |

---

## Tech Stack

**Backend**
- Java 21, Spring Boot 3.5.4 (Web, Data JPA, Data Redis, Validation, Actuator)
- PostgreSQL 16 (primary datastore) + Flyway (schema versioning)
- Redis 7 (leaderboard sorted set + report cache-aside layer)
- Lombok, JUnit 5, Mockito, Testcontainers

**Frontend**
- React 19 + React Router 7
- Tailwind CSS 4
- Recharts 3 (score distribution chart)
- Vite 8

**Infrastructure**
- Docker / Docker Compose
- Nginx (serves the production frontend build + SPA fallback routing)

---

## Architecture & Key Design Decisions

The system is intentionally split into two layers of "data conversion" so each part stays simple:

- **Flyway (`V1__init_schema.sql`, `V2__seed_reference_data.sql`)** owns the schema and the small, fixed reference tables (subject list, foreign-language codes) — anything that's part of the *structure* of the app.
- **`CsvScoreSeederService`** owns the large, one-time bulk import of the ~1M-row dataset — anything that's *data*, not structure. It runs as an opt-in `CommandLineRunner` (`app.seeder.enabled=true`) so starting the API for normal traffic never triggers a re-import.

Beyond the basic import, a few things were built to hold up under a dataset of this size:

- **Resumable seeding.** A `migration_checkpoint` table tracks `(file_name, last_line_offset, status)`. If the process is interrupted, restarting it resumes from the last committed batch instead of re-processing the whole file or duplicating rows (all inserts are idempotent via `ON CONFLICT`).
- **Batch inserts tuned for Postgres.** The seeder writes through raw JDBC `batchUpdate` with `reWriteBatchedInserts=true` on the JDBC URL, so ~1M rows import as multi-row batches instead of one round-trip per row.
- **Redis-backed leaderboard.** As each seed batch commits, Group A totals (Toán + Vật lí + Hóa học) are pushed into a Redis sorted set. Reads are served from Redis for speed, but:
  - the app only trusts Redis once the seed's checkpoint is `COMPLETED`;
  - a correct tie-break (`tổng điểm DESC, sbd ASC`) is enforced in application code, since Redis's own tie-break on ties would otherwise pick the wrong boundary candidate;
  - if Redis ever returns fewer than 10 members, that count is cross-checked against Postgres before being served as "the whole leaderboard" — otherwise a partial Redis state (crash, eviction, stray `ZREM`) could silently look complete.
  - If any of those checks fail, the leaderboard falls back to a direct Postgres aggregation query.
- **Report cache-aside.** Band counts per subject are cached in Redis with a 1-hour TTL, and — just like the leaderboard — are only written back to cache once the seed is verified complete, so a reader can never observe a partially-imported dataset cached as if it were final.
- **Clean layering.** Controller → Service → Repository throughout, with DTOs (Java records) at the API boundary and a single `@RestControllerAdvice` producing a consistent JSON error shape for the whole API.

---

## Project Structure

```
webdev-intern-assignment-3/
├── backend/                          Spring Boot API
│   └── src/main/java/com/goldenowl/gscores/
│       ├── config/                   CORS, seeder properties
│       ├── controller/               REST endpoints
│       ├── dto/                      API request/response records
│       ├── entity/                   JPA entities
│       ├── exception/                Global error handling
│       ├── repository/                Spring Data JPA repositories
│       ├── seeder/                   CSV parsing + resumable batch import
│       └── service/                  Business logic
│   └── src/main/resources/
│       └── db/migration/             Flyway SQL migrations
├── frontend/                          React SPA
│   └── src/
│       ├── api/                      Backend HTTP client
│       ├── components/               Layout, Sidebar, Header, Card, icons
│       └── pages/                    Lookup, Report, Leaderboard
├── dataset/
│   ├── diem_thi_thpt_2024.csv         Raw source data (1,061,605 rows)
│   └── diem_thi_thpt_2024_demo.csv    10,000-row sample, used for the free-tier live demo
└── docker-compose.yml
```

---

## Getting Started

### Option A — Docker Compose (recommended)

**Prerequisites:** Docker + Docker Compose.

```bash
git clone <this-repo-url>
cd webdev-intern-assignment-3
cp .env.example .env
```

Edit `.env` and set `APP_SEEDER_ENABLED=true` for the **first run only** — this tells the backend to import the CSV into Postgres on startup.

```bash
docker compose up --build
```

Watch the backend logs for `Seed completed for diem_thi_thpt_2024.csv`. Once that appears, the import is done and permanently recorded — the seeder is safe to leave enabled (it checks its own checkpoint and no-ops if already complete), but it's good practice to set `APP_SEEDER_ENABLED=false` afterward for faster restarts.

Once up:
- Frontend: **http://localhost:3000**
- Backend API: **http://localhost:8080**

To reset everything (including the seeded data):

```bash
docker compose down -v
```

### Option B — Run locally without Docker

**Prerequisites:** Java 21, Maven, Node.js 20+, a local PostgreSQL 16 instance, a local Redis 7 instance.

**1. Database & Redis**

Create a Postgres database matching `backend/src/main/resources/application.yml` (or override via environment variables):

```
Database: gscores
User:     gscores
Password: gscores
Port:     5432
```

Start Redis on the default port `6379`.

**2. Backend**

```bash
cd backend
./mvnw spring-boot:run
```

Flyway runs the schema migrations automatically on startup. To seed the dataset on this run only:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.seeder.enabled=true"
```

The API is now available at `http://localhost:8080`.

**3. Frontend**

```bash
cd frontend
cp .env.example .env   # adjust VITE_API_BASE_URL if the backend isn't on localhost:8080
npm install
npm run dev
```

The app is now available at `http://localhost:5173`.

---

## Environment Variables

**Root `.env`** (consumed by `docker-compose.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `APP_SEEDER_ENABLED` | `false` | Set to `true` on the first run to import the CSV dataset |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Origins allowed to call the API |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Baked into the frontend build at build time (Vite reads it at build, not at runtime) |

**`frontend/.env`** (for local `npm run dev`):

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend base URL used by the frontend HTTP client |

Backend connection details (Postgres/Redis host, port, credentials) default to `localhost` for local development and are overridden via standard Spring environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, etc.) inside `docker-compose.yml`.

---

## Database Schema

| Table | Purpose |
|---|---|
| `thi_sinh` | One row per candidate (`sbd` registration number, foreign-language choice) |
| `mon_thi` | Reference table of the 9 exam subjects |
| `ngoai_ngu` | Reference table of foreign-language codes (English, French, Chinese, ...) |
| `ket_qua_thi` | One row per (candidate, subject) score — the fact table the reports and leaderboard query |
| `migration_checkpoint` | Tracks CSV seeding progress so the import is resumable |

Two composite indexes back the two read-heavy queries directly:
- `(mon_thi_id, diem)` — for the per-subject band-count report
- `(mon_thi_id, thi_sinh_id, diem)` — for the Group A total-score aggregation

---

## API Reference

All endpoints are prefixed with `/api`. Errors follow a consistent shape:

```json
{
  "timestamp": "2026-07-05T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy thí sinh với SBD 01000001",
  "path": "/api/scores/01000001"
}
```

### `GET /api/scores/{sbd}`

Look up a candidate's full score sheet by 8-digit registration number.

```
GET /api/scores/01000001
```
```json
{
  "sbd": "01000001",
  "maNgoaiNgu": "N1",
  "tenNgoaiNgu": "Tiếng Anh",
  "scores": [
    { "maMon": "toan", "tenMon": "Toán", "diem": 8.4 },
    { "maMon": "ngu_van", "tenMon": "Ngữ văn", "diem": 6.75 }
  ]
}
```

### `GET /api/report/band-counts`

Number of candidates in each of the 4 score bands, per subject.

```json
[
  { "maMon": "toan", "tenMon": "Toán", "gioi": 120345, "kha": 340221, "trungBinh": 410120, "yeu": 190919 }
]
```

Score bands: `gioi` (Giỏi) `>= 8`, `kha` (Khá) `[6, 8)`, `trungBinh` (Trung bình) `[4, 6)`, `yeu` (Yếu) `< 4`.

### `GET /api/leaderboard/group-a`

Top 10 candidates by combined Toán + Vật lí + Hóa học score.

```json
[
  { "hang": 1, "sbd": "02123456", "tongDiem": 29.5 }
]
```

### `GET /api/subjects`

Reference list of the 9 exam subjects (`maMon`, `tenMon`).

---

## Testing

```bash
cd backend
./mvnw test
```

- Unit tests (JUnit 5 + Mockito) cover the report cache-aside and leaderboard tie-break/fallback logic.
- An integration test spins up a real PostgreSQL instance via Testcontainers to verify the Spring context and Flyway migrations load correctly.

Frontend linting:

```bash
cd frontend
npm run lint
```

---

## Deployment

The application is fully containerized: `docker-compose.yml` builds production images for both services (a JRE-slim runtime image for the backend, an Nginx-served static build for the frontend), so it can be deployed as-is to any container host by pointing `APP_CORS_ALLOWED_ORIGINS` and `VITE_API_BASE_URL` at the deployed domains.

The live demo linked above uses a free-tier split, since a single free-tier host generally can't fit both a Postgres+Redis instance and the ~1M-row dataset:

- **Postgres** → [Supabase](https://supabase.com) (free tier)
- **Backend + Redis** → [Railway](https://railway.app) (free tier)
- **Frontend** → [Vercel](https://vercel.com) (free tier)

Because free-tier Postgres/Redis storage is limited, the demo seeds `dataset/diem_thi_thpt_2024_demo.csv` (10,000 rows) instead of the full dataset. The seeder reads the CSV from the classpath, not a mounted volume, since Railway doesn't support host-mounted volumes like Docker Compose does — the demo CSV is already bundled at `backend/src/main/resources/dataset/diem_thi_thpt_2024_demo.csv` so it ships inside the built image.

### 1. Supabase (Postgres)

1. Create a new project at [supabase.com](https://supabase.com).
2. Grab the connection details from **Project Settings → Database** (host, port, database, user, password).
3. Flyway runs the schema migrations automatically on the backend's first startup — no manual SQL needed.

### 2. Railway (backend + Redis)

1. Create a new Railway project, add a **Redis** plugin, and add a service built from `backend/Dockerfile`.
2. Set these environment variables on the backend service:

   | Variable | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?reWriteBatchedInserts=true` |
   | `SPRING_DATASOURCE_USERNAME` | Supabase Postgres user |
   | `SPRING_DATASOURCE_PASSWORD` | Supabase Postgres password |
   | `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_PASSWORD` | From Railway's Redis plugin (`REDISHOST` / `REDISPORT` / `REDISPASSWORD`) |
   | `APP_SEEDER_ENABLED` | `true` for the first deploy only |
   | `APP_SEEDER_CSV_PATH` | `classpath:dataset/diem_thi_thpt_2024_demo.csv` |
   | `APP_SEEDER_FILE_NAME` | `diem_thi_thpt_2024_demo.csv` |
   | `APP_CORS_ALLOWED_ORIGINS` | Your Vercel URL, once known (step 3) |

3. Deploy, then watch the logs for `Seed completed for diem_thi_thpt_2024_demo.csv`. After that, set `APP_SEEDER_ENABLED=false` and redeploy — same as the local Docker Compose flow, just against a different CSV.
4. Note the public Railway backend URL for step 3.

### 3. Vercel (frontend)

1. Import `frontend/` as a Vercel project (framework preset: Vite).
2. Set the build-time environment variable `VITE_API_BASE_URL` to the Railway backend URL from step 2.
3. Deploy, then go back to Railway and set `APP_CORS_ALLOWED_ORIGINS` to the resulting Vercel URL (redeploy the backend so the change takes effect).
