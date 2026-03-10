package com.audita.audita.service;

import com.audita.audita.model.CategoryScore;
import com.audita.audita.model.CategoryScore.CheckItem;
import com.audita.audita.model.RepoData;
import com.audita.audita.model.RepoReport;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScoringService {

    public RepoReport score(RepoData data) {
        RepoReport report = new RepoReport();

        // Copy basic info
        report.setName(data.getName());
        report.setFullName(data.getFullName());
        report.setDescription(data.getDescription());
        report.setStars(data.getStars());
        report.setForks(data.getForks());
        report.setOpenIssues(data.getOpenIssues());
        report.setLastPushed(data.getLastPushed());
        report.setLicense(data.getLicense());
        report.setTopics(data.getTopics());
        report.setLanguages(data.getLanguages());
        report.setReadmeContent(data.getReadmeContent());
        report.setContributors(data.getContributors());
        report.setRootFiles(data.getRootFiles());

        List<CategoryScore> categories = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        categories.add(scoreDocumentation(data, suggestions));
        categories.add(scoreCICD(data, suggestions));
        categories.add(scoreWorkflowQuality(data, suggestions));
        categories.add(scoreDocker(data, suggestions));
        categories.add(scoreCodeQuality(data, suggestions));
        categories.add(scoreCommunity(data, suggestions));
        categories.add(scoreReleaseCadence(data, suggestions));

        int rawTotal = categories.stream().mapToInt(CategoryScore::getScore).sum();
        int maxTotal = categories.stream().mapToInt(CategoryScore::getMaxScore).sum();
        int normalizedScore = (int) Math.round((rawTotal / (double) maxTotal) * 100);
        report.setTotalScore(normalizedScore);
        report.setGrade(calculateGrade(normalizedScore));
        report.setCategories(categories);
        report.setSuggestions(suggestions);

        return report;
    }

    private CategoryScore scoreDocumentation(RepoData data, List<String> suggestions) {
        CategoryScore cat = new CategoryScore();
        cat.setName("Documentation");
        cat.setMaxScore(25);
        List<CheckItem> checks = new ArrayList<>();
        int score = 0;

        boolean hasReadme = hasFile(data, "README.md") || hasFile(data, "readme.md");
        if (hasReadme) {
            score += 8;
            checks.add(new CheckItem("README.md exists", "Found in root", "pass", 8));
        } else {
            checks.add(new CheckItem("No README.md found", "Highly recommended", "fail", 0));
            suggestions.add("Add a README.md explaining what the project does and how to run it.");
        }

        boolean hasLicense = hasFile(data, "LICENSE") || hasFile(data, "LICENSE.md") || hasFile(data, "LICENSE.txt");
        if (hasLicense) {
            score += 6;
            checks.add(new CheckItem("LICENSE file present", data.getLicense() != null ? data.getLicense() : "Found", "pass", 6));
        } else {
            checks.add(new CheckItem("No LICENSE file", "Required for open source", "fail", 0));
            suggestions.add("Add a LICENSE file to define how others can use your code.");
        }

        boolean hasContributing = hasFile(data, "CONTRIBUTING.md") || hasFile(data, "CONTRIBUTING");
        if (hasContributing) {
            score += 4;
            checks.add(new CheckItem("CONTRIBUTING.md present", "Guides contributors", "pass", 4));
        } else {
            checks.add(new CheckItem("No CONTRIBUTING.md", "Recommended for open source", "warn", 0));
            suggestions.add("Add a CONTRIBUTING.md to guide new contributors.");
        }

        boolean hasChangelog = hasFile(data, "CHANGELOG.md") || hasFile(data, "CHANGELOG") || hasFile(data, "HISTORY.md");
        if (hasChangelog) {
            score += 4;
            checks.add(new CheckItem("CHANGELOG present", "Tracks version history", "pass", 4));
        } else {
            checks.add(new CheckItem("No CHANGELOG found", "Helps track changes", "warn", 0));
            suggestions.add("Add a CHANGELOG.md to track version history and notable changes.");
        }

        boolean hasCodeOfConduct = hasFile(data, "CODE_OF_CONDUCT.md");
        if (hasCodeOfConduct) {
            score += 3;
            checks.add(new CheckItem("CODE_OF_CONDUCT.md present", "Community standards defined", "pass", 3));
        } else {
            checks.add(new CheckItem("No CODE_OF_CONDUCT.md", "Recommended for open source", "warn", 0));
        }

        cat.setScore(score);
        cat.setChecks(checks);
        return cat;
    }

    private CategoryScore scoreCICD(RepoData data, List<String> suggestions) {
        CategoryScore cat = new CategoryScore();
        cat.setName("CI/CD");
        cat.setMaxScore(25);
        List<CheckItem> checks = new ArrayList<>();
        int score = 0;

        if (data.isHasWorkflows()) {
            score += 15;
            checks.add(new CheckItem("GitHub Actions workflows found",
                    data.getWorkflowCount() + " workflow(s) detected", "pass", 15));
        } else {
            checks.add(new CheckItem("No GitHub Actions workflows", "No CI/CD automation found", "fail", 0));
            suggestions.add("Set up GitHub Actions to automate testing and building on every push.");
        }

        boolean hasGithubDir = hasFile(data, ".github");
        if (hasGithubDir) {
            score += 10;
            checks.add(new CheckItem(".github directory present", "Repo configuration exists", "pass", 10));
        } else {
            checks.add(new CheckItem("No .github directory", "Consider adding PR and issue templates", "warn", 0));
            suggestions.add("Add a .github directory with PR templates and issue templates.");
        }

        cat.setScore(score);
        cat.setChecks(checks);
        return cat;
    }

    private CategoryScore scoreDocker(RepoData data, List<String> suggestions) {
        CategoryScore cat = new CategoryScore();
        cat.setName("Docker");
        cat.setMaxScore(20);
        List<CheckItem> checks = new ArrayList<>();
        int score = 0;

        boolean hasDockerfile = hasFile(data, "Dockerfile") || hasFile(data, "dockerfile");
        if (hasDockerfile) {
            score += 10;
            checks.add(new CheckItem("Dockerfile found", "App is containerized", "pass", 10));
        } else {
            checks.add(new CheckItem("No Dockerfile found", "Consider containerizing your app", "fail", 0));
            suggestions.add("Add a Dockerfile to containerize your application for consistent deployments.");
        }

        boolean hasCompose = hasFile(data, "docker-compose.yml") || hasFile(data, "docker-compose.yaml");
        if (hasCompose) {
            score += 6;
            checks.add(new CheckItem("docker-compose.yml present", "Multi-container setup defined", "pass", 6));
        } else {
            checks.add(new CheckItem("No docker-compose.yml", "Useful for multi-service setups", "warn", 0));
            suggestions.add("Add a docker-compose.yml to define and run multi-container environments easily.");
        }

        boolean hasDockerIgnore = hasFile(data, ".dockerignore");
        if (hasDockerIgnore) {
            score += 4;
            checks.add(new CheckItem(".dockerignore present", "Docker build is optimized", "pass", 4));
        } else {
            checks.add(new CheckItem("No .dockerignore", "Keeps Docker images lean", "warn", 0));
        }

        cat.setScore(score);
        cat.setChecks(checks);
        return cat;
    }

    private CategoryScore scoreCodeQuality(RepoData data, List<String> suggestions) {
        CategoryScore cat = new CategoryScore();
        cat.setName("Code Quality");
        cat.setMaxScore(20);
        List<CheckItem> checks = new ArrayList<>();
        int score = 0;

        boolean hasGitignore = hasFile(data, ".gitignore");
        if (hasGitignore) {
            score += 5;
            checks.add(new CheckItem(".gitignore present", "Unwanted files excluded", "pass", 5));
        } else {
            checks.add(new CheckItem("No .gitignore found", "Important for clean repos", "fail", 0));
            suggestions.add("Add a .gitignore to prevent committing build artifacts and secrets.");
        }

        boolean hasLinting = hasFile(data, ".eslintrc.js") || hasFile(data, ".eslintrc")
                || hasFile(data, ".eslintrc.json") || hasFile(data, "checkstyle.xml")
                || hasFile(data, ".pylintrc") || hasFile(data, ".rubocop.yml");
        if (hasLinting) {
            score += 7;
            checks.add(new CheckItem("Linting config detected", "Code style is enforced", "pass", 7));
        } else {
            checks.add(new CheckItem("No linting config found", "Consider adding a linter", "warn", 0));
            suggestions.add("Add a linting configuration to enforce consistent code style.");
        }

        boolean hasSecurity = hasFile(data, "SECURITY.md") || hasFile(data, "SECURITY");
        if (hasSecurity) {
            score += 8;
            checks.add(new CheckItem("SECURITY.md present", "Vulnerability reporting defined", "pass", 8));
        } else {
            checks.add(new CheckItem("No SECURITY.md", "Define how to report vulnerabilities", "warn", 0));
            suggestions.add("Add a SECURITY.md to explain how to responsibly report vulnerabilities.");
        }

        cat.setScore(score);
        cat.setChecks(checks);
        return cat;
    }

    private CategoryScore scoreCommunity(RepoData data, List<String> suggestions) {
        CategoryScore cat = new CategoryScore();
        cat.setName("Community");
        cat.setMaxScore(10);
        List<CheckItem> checks = new ArrayList<>();
        int score = 0;

        if (data.getDescription() != null && !data.getDescription().isBlank()) {
            score += 3;
            checks.add(new CheckItem("Repository has a description", "Clear purpose stated", "pass", 3));
        } else {
            checks.add(new CheckItem("No description set", "Add a short repo description", "warn", 0));
            suggestions.add("Add a repository description so visitors immediately understand the project.");
        }

        if (data.getTopics() != null && !data.getTopics().isEmpty()) {
            score += 3;
            checks.add(new CheckItem("Topics/tags configured",
                    String.join(", ", data.getTopics().subList(0, Math.min(3, data.getTopics().size()))),
                    "pass", 3));
        } else {
            checks.add(new CheckItem("No topics set", "Helps with discoverability", "warn", 0));
            suggestions.add("Add topics to your repository to improve discoverability.");
        }

        try {
            Instant lastPush = Instant.parse(data.getLastPushed());
            long daysSince = ChronoUnit.DAYS.between(lastPush, Instant.now());
            if (daysSince <= 30) {
                score += 4;
                checks.add(new CheckItem("Actively maintained", "Last commit " + daysSince + " day(s) ago", "pass", 4));
            } else if (daysSince <= 180) {
                score += 2;
                checks.add(new CheckItem("Moderately active", "Last commit " + daysSince + " days ago", "warn", 2));
            } else {
                checks.add(new CheckItem("Inactive repository", "No commits in " + daysSince + " days", "fail", 0));
                suggestions.add("Repository appears inactive. Regular commits improve project credibility.");
            }
        } catch (Exception e) {
            checks.add(new CheckItem("Could not determine activity", "", "warn", 0));
        }

        cat.setScore(score);
        cat.setChecks(checks);
        return cat;
    }

    private CategoryScore scoreWorkflowQuality(RepoData data, List<String> suggestions) {
    CategoryScore cat = new CategoryScore();
    cat.setName("Workflow Quality");
    cat.setMaxScore(20);
    List<CheckItem> checks = new ArrayList<>();
    int score = 0;

    if (!data.isHasWorkflows() || data.getWorkflowContents().isEmpty()) {
        checks.add(new CheckItem("No workflows to analyze", "Add GitHub Actions first", "fail", 0));
        cat.setScore(0);
        cat.setChecks(checks);
        return cat;
    }

    String allContents = String.join("\n", data.getWorkflowContents());

    boolean hasCaching = allContents.contains("cache") || allContents.contains("actions/cache");
    if (hasCaching) {
        score += 5;
        checks.add(new CheckItem("Dependency caching configured", "Speeds up build times", "pass", 5));
    } else {
        checks.add(new CheckItem("No caching in workflows", "Add caching to speed up CI", "warn", 0));
        suggestions.add("Add dependency caching in your GitHub Actions workflows to reduce build time by up to 60%.");
    }

    boolean runOnPR = allContents.contains("pull_request");
    if (runOnPR) {
        score += 5;
        checks.add(new CheckItem("Workflows trigger on pull requests", "PRs are validated automatically", "pass", 5));
    } else {
        checks.add(new CheckItem("No PR trigger found", "Workflows should run on pull requests", "warn", 0));
        suggestions.add("Configure your workflows to trigger on pull_request events to validate code before merging.");
    }

    boolean hasTimeout = allContents.contains("timeout-minutes");
    if (hasTimeout) {
        score += 5;
        checks.add(new CheckItem("Timeout configured in workflows", "Prevents runaway jobs", "pass", 5));
    } else {
        checks.add(new CheckItem("No timeout-minutes set", "Jobs could run indefinitely", "warn", 0));
        suggestions.add("Add timeout-minutes to your workflow jobs to prevent runaway CI builds.");
    }

    boolean hasPinnedVersions = allContents.matches("(?s).*uses: [a-zA-Z0-9_/-]+@[a-f0-9]{6,}.*");
    if (hasPinnedVersions) {
        score += 5;
        checks.add(new CheckItem("Action versions are pinned", "Supply chain security best practice", "pass", 5));
    } else {
        checks.add(new CheckItem("Actions not pinned to commit SHA", "Pin versions for security", "warn", 0));
        suggestions.add("Pin your GitHub Actions to specific commit SHAs instead of tags for better supply chain security.");
    }

    cat.setScore(score);
    cat.setChecks(checks);
    return cat;
}

private CategoryScore scoreReleaseCadence(RepoData data, List<String> suggestions) {
    CategoryScore cat = new CategoryScore();
    cat.setName("Release Cadence");
    cat.setMaxScore(10);
    List<CheckItem> checks = new ArrayList<>();
    int score = 0;

    if (data.getTotalReleases() == 0) {
        checks.add(new CheckItem("No releases found", "Start tagging releases", "fail", 0));
        suggestions.add("Create GitHub Releases to track versions and give users a clear history of changes.");
        cat.setScore(0);
        cat.setChecks(checks);
        return cat;
    }

    score += 4;
    checks.add(new CheckItem("Releases exist",
            data.getTotalReleases() + " release(s) found", "pass", 4));

    // Check how recent the latest release is
    try {
        long daysSinceRelease = java.time.temporal.ChronoUnit.DAYS.between(
            java.time.Instant.parse(data.getLatestReleaseDate()),
            java.time.Instant.now()
        );

        if (daysSinceRelease <= 90) {
            score += 3;
            checks.add(new CheckItem("Recent release found",
                    "Last release " + daysSinceRelease + " days ago", "pass", 3));
        } else {
            checks.add(new CheckItem("Release is outdated",
                    "Last release " + daysSinceRelease + " days ago", "warn", 0));
            suggestions.add("Your last release was over 90 days ago. Regular releases improve user trust.");
        }
    } catch (Exception e) {
        checks.add(new CheckItem("Could not determine release date", "", "warn", 0));
    }

    // Check release frequency
    if (data.getAvgDaysBetweenReleases() > 0) {
        double avg = data.getAvgDaysBetweenReleases();
        if (avg <= 30) {
            score += 3;
            checks.add(new CheckItem("Frequent release cadence",
                    String.format("Avg %.0f days between releases", avg), "pass", 3));
        } else if (avg <= 90) {
            score += 2;
            checks.add(new CheckItem("Moderate release cadence",
                    String.format("Avg %.0f days between releases", avg), "warn", 2));
        } else {
            checks.add(new CheckItem("Infrequent releases",
                    String.format("Avg %.0f days between releases", avg), "fail", 0));
        }
    }

    cat.setScore(score);
    cat.setChecks(checks);
    return cat;
}

    private boolean hasFile(RepoData data, String filename) {
        if (data.getRootFiles() == null) return false;
        return data.getRootFiles().stream()
                .anyMatch(f -> f.equalsIgnoreCase(filename));
    }

    private String calculateGrade(int score) {
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 55) return "C";
        return "D";
    }
}