# ⬡ Audita

**Audit any public GitHub repository instantly.**

Audita analyses a GitHub repo across 7 categories and returns a scored health report — CI/CD setup, documentation quality, Docker configuration, code quality signals, community health, workflow best practices, and release cadence. Results in seconds, exportable as PDF.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square)
![License](https://img.shields.io/github/license/shreeshasn/Audita?style=flat-square)

---

## Features

- **7-category scoring** — Documentation, CI/CD, Docker, Code Quality, Community, Workflow Quality, Release Cadence
- **Animated score ring** with grade badge (A–D)
- **Improvement suggestions** with direct links to relevant docs
- **Analytics tab** — language breakdown, top contributors, folder structure, README preview
- **Side-by-side comparison** — compare two repos head to head with a winner banner
- **Shareable links** — every report gets a `/r/owner/repo` URL
- **PDF export** — full dark-themed report download
- **Cold-start aware UI** — waking indicator, rotating quotes, branded shared-link hold screen

---

## Tech Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3, WebFlux WebClient |
| Frontend | Vanilla HTML / CSS / JS (no framework) |
| PDF | jsPDF + html2canvas |
| Markdown | marked.js |
| Data source | GitHub REST API v3 |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- A GitHub Personal Access Token (classic, `public_repo` scope is enough)

### Setup

```bash
git clone https://github.com/shreeshasn/Audita.git
cd Audita
cp application.properties.example src/main/resources/application.properties
```

Edit `application.properties` and add your token:

```properties
github.token=your_token_here
github.base-url=https://api.github.com
server.port=8081
```

### Run

```bash
mvn spring-boot:run
```

Open `http://localhost:8081`

---

## Project Structure

```
src/
├── main/
│   ├── java/com/audita/audita/
│   │   ├── controller/
│   │   │   ├── RepoController.java      # /api/analyze endpoint
│   │   │   ├── ShareController.java     # /r/** → index.html
│   │   │   └── HealthController.java    # /health ping
│   │   ├── service/
│   │   │   ├── GitHubService.java       # GitHub API calls
│   │   │   └── ScoringService.java      # Scoring logic
│   │   └── model/
│   │       ├── RepoData.java
│   │       ├── RepoReport.java
│   │       └── CategoryScore.java
│   └── resources/
│       ├── static/
│       │   ├── index.html
│       │   ├── css/                     # theme.css, index.css, compare.css
│       │   ├── js/                      # utils.js, index.js, compare.js
│       │   └── pages/
│       │       └── compare.html
│       └── application.properties      # gitignored — use .example
```

---

## Scoring Breakdown

| Category | Max | Key Checks |
|---|---|---|
| Documentation | 25 | README, LICENSE, CONTRIBUTING, CHANGELOG, CODE_OF_CONDUCT |
| CI/CD | 25 | GitHub Actions workflows, .github directory |
| Workflow Quality | 20 | Caching, PR trigger, timeout, pinned action SHAs |
| Docker | 20 | Dockerfile, docker-compose, .dockerignore |
| Code Quality | 20 | .gitignore, linting config, SECURITY.md |
| Community | 10 | Description, topics, recent activity |
| Release Cadence | 10 | Releases exist, recency, frequency |

---

## Deployment

The app is deployed on Render. A `Dockerfile` is included for containerised deployments.

```bash
docker build -t audita .
docker run -p 8080:8080 \
  -e GITHUB_TOKEN=your_token \
  audita
```

---

## License

MIT — see [LICENSE](LICENSE)