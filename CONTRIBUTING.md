# Contributing to Audita

Thanks for your interest in contributing. Here's how to get started.

## Setup

```bash
git clone https://github.com/YOUR_USERNAME/audita.git
cd audita
cp application.properties.example src/main/resources/application.properties
# Add your GitHub token to application.properties
mvn spring-boot:run
```

The app runs on `http://localhost:8081`.

## Making Changes

1. Fork the repo and create a branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Test locally — run the app and try it against a few repos
4. Open a pull request with a clear description of what changed and why

## What's Welcome

- New scoring categories or checks in `ScoringService.java`
- UI improvements to `index.html`, `compare.html`, or the CSS/JS files
- Bug fixes — especially edge cases in GitHub API handling
- Documentation improvements

## Code Style

- Java: follow the existing patterns, checkstyle.xml enforces the basics
- JS/CSS: match the existing naming conventions and structure

## Questions

Open a GitHub issue with the `question` label.
