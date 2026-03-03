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

        List<CategoryScore> categories = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        categories.add(scoreDocumentation(data, suggestions));
        categories.add(scoreCICD(data, suggestions));
        categories.add(scoreDocker(data, suggestions));
        categories.add(scoreCodeQuality(data, suggestions));
        categories.add(scoreCommunity(data, suggestions));

        int total = categories.stream().mapToInt(CategoryScore::getScore).sum();
        report.setTotalScore(total);
        report.setGrade(calculateGrade(total));
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