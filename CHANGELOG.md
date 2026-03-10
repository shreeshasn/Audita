# Changelog

All notable changes to Audita are documented here.

## [Unreleased]

### Added
- Cold-start UX: global `apiFetch` wrapper with 5s waking pill indicator
- Rotating quotes during analysis loading screen
- Branded holding screen for shared `/r/owner/repo` links
- `/health` endpoint for deployment monitoring
- Compare page: per-repo error messages on fetch failure

### Fixed
- Private/missing repos now return clear error messages instead of 500
- 401/403 GitHub API errors surfaced with human-readable messages
- URL validation in `RepoController` before hitting GitHub API

## [1.0.0] - 2025-03-01

### Added
- Core repo analysis across 7 categories: Documentation, CI/CD, Docker, Code Quality, Community, Workflow Quality, Release Cadence
- Score ring with animated grade badge
- Suggestions panel with Learn links
- Analytics tab: languages, contributors, folder structure, README preview
- Side-by-side repo comparison with winner banner and category table
- Shareable `/r/owner/repo` URLs
- PDF export
- Landing page typewriter animation
