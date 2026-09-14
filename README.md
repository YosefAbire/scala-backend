# 🔴 HiCenter Scala Backend API

High-performance, type-safe REST API server for the HiCenter platform, built with **Scala 3** and **Play Framework 3**.

Repository: [https://github.com/YosefAbire/scala-backend.git](https://github.com/YosefAbire/scala-backend.git)

---

## 🚀 Overview

The Scala backend serves as the core API engine for HiCenter, powering:
- **User Authentication**: Secure JWT token generation, refresh mechanism, cookie handling, and BCrypt password encryption.
- **HiTime Sanctuary**: Time management, priority task queue (Now / Next / Later), study routines, and focus session management.
- **HiSchool Hub**: Verified subject notes, chapter quizzes, peer study circles, and graduate pathway guidance.
- **School Roster Management**: Multi-tenant school registration, administrator provisioning, and Grade 11–12 student roster controls.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Scala 3 (`3.3.3`)
- **Web Framework**: Play Framework 3 (`PlayScala`)
- **Dependency Injection**: Guice DI
- **Authentication & Security**:
  - `com.github.jwt-scala` (`jwt-play-json` `10.0.0`)
  - `org.mindrot` (`jbcrypt` `0.4`)
- **Database Driver**: PostgreSQL (`42.7.3`) / In-Memory Repository Layer
- **Build Tool**: sbt 1.9+

---

## 📂 Project Structure

```
scala-backend/
├── app/
│   ├── auth/                # JWT Token service & claims handling
│   │   └── JwtService.scala
│   ├── controllers/         # Play HTTP controllers
│   │   ├── AuthController.scala
│   │   ├── HealthController.scala
│   │   ├── HiSchoolController.scala
│   │   ├── HiTimeController.scala
│   │   └── SchoolController.scala
│   ├── domain/              # Case classes, DTOs & JSON formatters
│   │   ├── Dtos.scala
│   │   └── Models.scala
│   └── repositories/        # Data access repositories
│       ├── HiSchoolRepository.scala
│       ├── HiTimeRepository.scala
│       ├── SchoolRepository.scala
│       └── UserRepository.scala
├── conf/
│   ├── application.conf     # Server & CORS configurations
│   └── routes               # HTTP route declarations
├── project/                 # sbt build definitions
│   ├── build.properties
│   └── plugins.sbt
├── test/                    # Controller unit & integration tests
│   └── controllers/
│       └── HealthControllerSpec.scala
└── build.sbt                # Project dependencies & Scala 3 configuration
```

---

## 📡 API Endpoints Summary

### 1. Health & Diagnostics
| Method | Route | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health/` | Service health status check |

### 2. Authentication (`/api/auth/`)
| Method | Route | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login/` | Authenticate user & issue JWT cookies/tokens |
| `POST` | `/api/auth/logout/` | Revoke session & clear HttpOnly tokens |
| `POST` | `/api/auth/refresh/` | Issue new access token from valid refresh token |
| `GET` | `/api/auth/me/` | Fetch active user session DTO |
| `POST` | `/api/auth/activate/` | Set initial password for school-invited account |

### 3. Schools & Roster Management (`/api/schools/`)
| Method | Route | Description |
| :--- | :--- | :--- |
| `GET` | `/api/schools/` | List registered schools |
| `POST` | `/api/schools/` | Provision a new school |
| `GET` | `/api/schools/:id/` | Retrieve school summary by ID |
| `POST` | `/api/schools/:id/admins/` | Invite school administrator |
| `GET` | `/api/schools/:id/roster/` | Get student & faculty roster for school |
| `POST` | `/api/schools/:id/students/` | Enroll new student |

### 4. HiSchool Learning Hub (`/api/hischool/`)
| Method | Route | Description |
| :--- | :--- | :--- |
| `GET` | `/api/hischool/notes/` | Fetch verified subject study notes |
| `POST` | `/api/hischool/notes/` | Publish new study note |
| `GET` | `/api/hischool/quizzes/` | Fetch diagnostic chapter quizzes |
| `GET` | `/api/hischool/circles/` | List live peer study circles |
| `GET` | `/api/hischool/pathways/` | List graduate pathway guides |

### 5. HiTime Focus Sanctuary (`/api/hitime/`)
| Method | Route | Description |
| :--- | :--- | :--- |
| `GET` | `/api/hitime/tasks/` | Fetch active tasks (Now/Next/Later) |
| `POST` | `/api/hitime/tasks/` | Create a new task item |
| `PATCH` | `/api/hitime/tasks/:id/` | Update task completion state |
| `DELETE` | `/api/hitime/tasks/:id/` | Remove task |
| `GET` | `/api/hitime/routines/` | Fetch morning/evening checklist routines |
| `GET` | `/api/hitime/sessions/` | Fetch focus timer session logs |

---

## ⚙️ Prerequisites & Setup

### Requirements
- **Java Development Kit (JDK)**: Java 17 or higher
- **sbt**: version 1.9.0 or higher

### Build & Run Commands

```bash
# 1. Compile project sources
sbt compile

# 2. Run unit tests
sbt test

# 3. Start development server (Port 9000)
sbt run
```

---

## 🔗 Integration with Next.js Frontend

The Next.js frontend connects directly to this server via proxy rewrites in `frontend/next.config.ts`:

```typescript
// next.config.ts
const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://127.0.0.1:9000/api/:path*",
      },
    ];
  },
};
```

---

## 📜 License
Part of the **HiCenter** platform ecosystem.
