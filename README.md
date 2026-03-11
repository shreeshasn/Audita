<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=00ff88&height=120&section=header&text=⬡ AUDITA&fontSize=52&fontColor=ffffff&fontAlignY=65&animation=fadeIn" width="100%"/>

<br/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=16&duration=3000&pause=1000&color=00FF88&center=true&vCenter=true&width=600&lines=Audit+any+public+GitHub+repository+instantly.;7+categories.+Scored+out+of+100.+Graded+A–D.;CI%2FCD+%E2%80%A2+Docker+%E2%80%A2+Docs+%E2%80%A2+Code+Quality+%E2%80%A2+Community;Share+reports.+Export+PDF.+Compare+repos.)](https://git.io/typing-svg)

<br/>

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Deployed on Render](https://img.shields.io/badge/Deployed_on-Render-46E3B7?style=flat-square&logo=render&logoColor=white)](https://audita.onrender.com)
[![License](https://img.shields.io/github/license/shreeshasn/Audita?style=flat-square&color=blue)](LICENSE)
[![Visitors](https://visitor-badge.laobi.icu/badge?page_id=shreeshasn.Audita&style=flat-square&color=00ff88)](https://github.com/shreeshasn/Audita)

<br/>

**[🚀 Try it live](https://audita-nkss.onrender.com)** · **[📖 API docs](#api)** · **[🛠 Run locally](#getting-started)**

</div>

---

## What it does

Audita analyses any public GitHub repository across **7 weighted categories** and returns a normalised score out of 100 with a letter grade. Every check is transparent — you see exactly what passed, what failed, and what to fix.

<details>
<summary><strong>🔍 All 7 categories and their checks</strong></summary>

<br/>

| Category | Weight | What's checked |
|---|---|---|
| 📄 **Documentation** | 25 pts | README, LICENSE, CONTRIBUTING, CHANGELOG, CODE_OF_CONDUCT |
| ⚙️ **CI/CD** | 25 pts | GitHub Actions workflows, `.github` directory present |
| 🔁 **Workflow Quality** | 20 pts | Action caching, PR trigger, timeout-minutes, pinned SHA versions |
| 🐳 **Docker** | 20 pts | Dockerfile, docker-compose, .dockerignore |
| 🧪 **Code Quality** | 20 pts | .gitignore, linting config, SECURITY.md |
| 🌍 **Community** | 10 pts | Description, topics configured, recent commit activity |
| 🚀 **Release Cadence** | 10 pts | Releases exist, recency, frequency |

</details>

<details>
<summary><strong>✨ Full feature list</strong></summary>

<br/>

- **Animated score ring** with live counter and grade badge (A–D) coloured by result
- **Per-category breakdown** — every check shown as pass / warn / fail with explanation
- **Actionable suggestions** — improvement tips with direct links to GitHub docs
- **Analytics tab** — language chart, top contributors, root folder structure, README preview with markdown rendering
- **Side-by-side compare** — two repos head to head, winner banner, per-category delta table
- **Shareable links** — every report lives at `/r/owner/repo`, shareable without re-running
- **PDF export** — full dark-themed report including score ring, category breakdown, and suggestions
- **Cold-start aware UI** — waking pill indicator, escalating messages after 20s, rotating quotes during load, branded hold screen for shared links

</details>

---

## Backend Architecture

<details>
<summary><strong>📐 How the analysis pipeline works (click to expand)</strong></summary>

<br/>

Every request flows through four layers:

```
Browser → RepoController → GitHubService → ScoringService → JSON → Frontend
                ↑                ↑
           validation         @Cacheable
           + error map        15min TTL
```

---

### Layer 1 — RepoController

Entry point for all analysis requests at `GET /api/analyze?repoUrl=`.

- Validates the URL is a well-formed `github.com` path parseable into `owner/repo`
- Returns `400` with a human-readable `{ "error": "..." }` JSON body on bad input
- Wraps the full pipeline in try/catch:
  - `RuntimeException` (expected: private repo, bad URL, rate limit) → `400`
  - Anything else → `500`
- Returns `ResponseEntity<?>` so error shapes and success shapes can differ

---

### Layer 2 — GitHubService

Makes **8 sequential GitHub REST API calls** using Spring WebFlux `WebClient`. Each call uses `.block()` to resolve synchronously inside a standard Spring MVC thread.

| # | Endpoint | Purpose |
|---|---|---|
| 1 | `GET /repos/{owner}/{repo}` | Core metadata — name, description, stars, forks, issues, license, topics, pushed_at |
| 2 | `GET /repos/{owner}/{repo}/contents` | Root file listing — detects README, LICENSE, Dockerfile, .gitignore, etc. |
| 3 | `GET /repos/{owner}/{repo}/languages` | Language byte counts — used for the language breakdown chart |
| 4 | `GET /repos/{owner}/{repo}/contributors` | Top contributors with commit counts |
| 5 | `GET /repos/{owner}/{repo}/contents/.github/workflows` | Lists workflow YAML files |
| 6 | `GET` each workflow file | Fetches full YAML content for quality checks (caching, PR trigger, timeout, SHA pinning) |
| 7 | `GET /repos/{owner}/{repo}/releases` | Release list with timestamps for cadence scoring |
| 8 | `GET /repos/{owner}/{repo}/readme` | Base64-encoded README content, decoded for the analytics preview |

**Error handling:** Only Call 1 is fatal — if the repo doesn't exist or the token is invalid, it throws immediately with a mapped message. Calls 2–8 fail silently — scoring simply gives zero for any data that couldn't be fetched.

Call 1 HTTP status → error message mapping:
```
404 → "Repository not found. It may be private or the URL is misspelled."
401 → "GitHub token is invalid or expired."
403 → "GitHub API rate limit exceeded. Try again in a few minutes."
5xx → "GitHub API returned an error. Try again shortly."
```

---

### Layer 3 — ScoringService

Receives a populated `RepoData` object and runs **7 independent scoring methods**, one per category.

Each method returns a `CategoryScore` containing:
- A raw integer score
- A max possible score for the category
- A list of `CheckItem` objects — each with a label, status (`PASS` / `WARN` / `FAIL`), and explanation

Final score normalisation:
```java
int totalScore = (int) Math.round((rawTotal / (double) maxTotal) * 100);
```

Grade thresholds:
```
A  ≥ 85 pts   (Excellent)
B  ≥ 70 pts   (Good)
C  ≥ 55 pts   (Fair)
D  < 55 pts   (Needs work)
```

---

### Layer 4 — Caching (CacheConfig + @Cacheable)

Caffeine in-memory cache sits in front of `GitHubService.fetchRepoData()`.

```java
@Cacheable(value = "reports", key = "#owner + '/' + #repo")
public RepoData fetchRepoData(String owner, String repo) { ... }
```

Behaviour:
- **Cache hit** (same `owner/repo` within TTL) → returns immediately from memory, zero GitHub API calls
- **Cache miss** → runs full 8-call pipeline, stores result
- **TTL:** 15 minutes — after expiry the next request runs fresh
- **Max size:** 50 entries — least-recently-used entries evicted automatically when exceeded
- **Persistence:** in-memory only — cache is empty on every container start

This makes the compare page fast (if one repo was recently analysed, it's a cache hit) and shared links near-instant for repeat visitors within the TTL window.

</details>

---

## Tech Stack

<div align="center">

[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/)
[![WebFlux](https://img.shields.io/badge/WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
[![Caffeine](https://img.shields.io/badge/Caffeine_Cache-F7DF1E?style=for-the-badge&logo=coffeescript&logoColor=black)](https://github.com/ben-manes/caffeine)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)](https://render.com/)

</div>

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, WebFlux WebClient |
| Caching | Caffeine — in-memory, 15min TTL, 50 entry max |
| Frontend | Vanilla HTML / CSS / JS — zero frameworks |
| PDF generation | jsPDF + html2canvas (client-side) |
| Markdown rendering | marked.js |
| Data source | GitHub REST API v3 |
| Container | Docker (multi-stage build, JVM tuned for 512MB) |
| Hosting | Render free tier + UptimeRobot keep-alive |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- A GitHub Personal Access Token — [generate one here](https://github.com/settings/tokens) *(classic token, `public_repo` scope is sufficient)*

### Local Setup

```bash
# 1. Clone the repository
git clone https://github.com/shreeshasn/Audita.git
cd Audita

# 2. Set your GitHub token as an environment variable

# Windows — PowerShell (persistent, survives terminal restarts):
[System.Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "your_token_here", "User")
# Close and reopen your terminal after running this

# macOS / Linux:
export GITHUB_TOKEN=your_token_here

# 3. Run
mvn spring-boot:run
```

Open [http://localhost:8081](http://localhost:8081)

### Run with Docker

```bash
# Build
docker build -t audita .

# Run
docker run -p 8080:8080 -e GITHUB_TOKEN=your_token_here audita
```

Open [http://localhost:8080](http://localhost:8080)

---

## Folder Structure

```
audita/
├── Dockerfile                            # Multi-stage build, JVM tuned for 512MB RAM
├── .dockerignore
├── checkstyle.xml                        # Java code style rules
├── SECURITY.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── CODE_OF_CONDUCT.md
├── application.properties.example        # Template — copy to src/main/resources/
│
└── src/main/
    ├── java/com/audita/audita/
    │   ├── AuditaApplication.java
    │   ├── config/
    │   │   └── CacheConfig.java          # Caffeine cache — 15min TTL, max 50 entries
    │   ├── controller/
    │   │   ├── RepoController.java       # GET /api/analyze — validation + pipeline
    │   │   ├── ShareController.java      # GET /r/** → forward to index.html
    │   │   └── HealthController.java     # GET /health → {"status":"ok"}
    │   ├── service/
    │   │   ├── GitHubService.java        # 8 GitHub API calls + @Cacheable
    │   │   └── ScoringService.java       # 7-category scoring engine
    │   └── model/
    │       ├── RepoData.java             # Raw GitHub data (input to scoring)
    │       ├── RepoReport.java           # Final scored report (returned to frontend)
    │       └── CategoryScore.java        # Per-category score + check items
    │
    └── resources/
        ├── application.properties        # Env var placeholders — safe to commit
        └── static/
            ├── index.html                # Landing + analysis page (pure HTML shell)
            ├── css/
            │   ├── theme.css             # CSS variables, grid bg, shared components, pill
            │   ├── index.css             # Analysis page styles, quotes, hold screen
            │   └── compare.css           # Compare page styles
            ├── js/
            │   ├── utils.js              # API_BASE, helpers, apiFetch with cold-start UX
            │   ├── index.js              # Full analysis flow, PDF, share, typewriter
            │   └── compare.js            # Compare flow and render logic
            └── pages/
                └── compare.html          # Compare page (pure HTML shell)
```

---

## Deployment

Audita runs on [Render](https://render.com) via Docker. Render auto-deploys on every push to `main`.

The container is kept warm by [UptimeRobot](https://uptimerobot.com) pinging `/health` every 5 minutes — this prevents Render's free tier from spinning down and causing 50+ second cold starts.

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GITHUB_TOKEN` | ✅ | — | GitHub personal access token |
| `GITHUB_BASE_URL` | ❌ | `https://api.github.com` | GitHub API base — override for testing |
| `PORT` | ❌ | `8081` | Server port — Render injects this automatically |

---

## API Reference

### `GET /api/analyze`

Analyses a public GitHub repository and returns a full scored report.

**Query parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoUrl` | string | ✅ | Full GitHub repo URL — `https://github.com/owner/repo` |

**Success response — `200 OK`**
```json
{
  "name": "Audita",
  "fullName": "shreeshasn/Audita",
  "description": "Audit any public GitHub repository instantly.",
  "totalScore": 93,
  "grade": "A",
  "stars": 0,
  "forks": 0,
  "openIssues": 0,
  "lastPushed": "2026-03-10T...",
  "categories": [
    {
      "name": "Documentation",
      "score": 25,
      "maxScore": 25,
      "checks": [
        { "label": "README present", "status": "PASS", "detail": "..." }
      ]
    }
  ],
  "suggestions": ["..."],
  "languages": { "Java": 18432, "HTML": 4200 },
  "contributors": [{ "login": "shreeshasn", "contributions": 42 }],
  "rootFiles": ["README.md", "Dockerfile", "pom.xml"]
}
```

**Error responses — `400 Bad Request`**
```json
{ "error": "Repository not found. It may be private or the URL is misspelled." }
{ "error": "GitHub API rate limit exceeded. Try again in a few minutes." }
{ "error": "Invalid GitHub URL. Expected format: https://github.com/owner/repo" }
```

---

### `GET /health`

Returns `{"status":"ok"}`. Used by UptimeRobot to keep the container warm.

---

<img src="https://capsule-render.vercel.app/api?type=waving&color=00ff88&height=80&section=footer" width="100%"/>