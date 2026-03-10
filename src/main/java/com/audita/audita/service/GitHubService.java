package com.audita.audita.service;

import com.audita.audita.model.RepoData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService {

    private final WebClient webClient;

    public GitHubService(@Value("${github.token}") String token,
                         @Value("${github.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public RepoData fetchRepoData(String owner, String repo) {
        RepoData data = new RepoData();

        // ── Call 1 — basic repo info (FATAL if this fails) ──────────────────
        // GitHub returns 404 for both missing AND private repos intentionally,
        // to avoid leaking whether a private repo exists.
        // 401 = bad/expired token. 403 = rate-limited or blocked.
        JsonNode repoInfo;
        try {
            repoInfo = webClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) {
                throw new RuntimeException(
                    "Repository not found. It may be private or the URL is misspelled. " +
                    "Audita can only analyse public repositories.");
            } else if (status == 401) {
                throw new RuntimeException(
                    "GitHub authentication failed. The server token may be invalid or expired.");
            } else if (status == 403) {
                throw new RuntimeException(
                    "GitHub API rate limit exceeded or access forbidden. Please try again in a few minutes.");
            } else {
                throw new RuntimeException(
                    "GitHub returned an unexpected error (HTTP " + status + ") for '" + owner + "/" + repo + "'.");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Could not reach the GitHub API. Check your connection and try again.");
        }

        if (repoInfo == null) {
            throw new RuntimeException(
                "GitHub returned an empty response for '" + owner + "/" + repo + "'. Please try again.");
        }

        data.setName(repoInfo.path("name").asText());
        data.setFullName(repoInfo.path("full_name").asText());
        data.setDescription(repoInfo.path("description").asText());
        data.setStars(repoInfo.path("stargazers_count").asInt());
        data.setForks(repoInfo.path("forks_count").asInt());
        data.setOpenIssues(repoInfo.path("open_issues_count").asInt());
        data.setLastPushed(repoInfo.path("pushed_at").asText());

        JsonNode licenseNode = repoInfo.path("license");
        if (!licenseNode.isMissingNode() && !licenseNode.isNull()) {
            data.setLicense(licenseNode.path("name").asText());
        }

        List<String> topics = new ArrayList<>();
        repoInfo.path("topics").forEach(t -> topics.add(t.asText()));
        data.setTopics(topics);

        // ── Call 2 — root file listing (non-fatal) ───────────────────────────
        List<String> rootFiles = new ArrayList<>();
        try {
            JsonNode contents = webClient.get()
                    .uri("/repos/{owner}/{repo}/contents", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (contents != null && contents.isArray()) {
                contents.forEach(file -> rootFiles.add(file.path("name").asText()));
            }
        } catch (Exception e) { /* empty or inaccessible contents — non-fatal */ }
        data.setRootFiles(rootFiles);

        // ── Call 3 — workflows list (non-fatal) ──────────────────────────────
        List<String> workflowContents = new ArrayList<>();
        try {
            JsonNode workflows = webClient.get()
                    .uri("/repos/{owner}/{repo}/actions/workflows", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (workflows != null) {
                int count = workflows.path("total_count").asInt();
                data.setWorkflowCount(count);
                data.setHasWorkflows(count > 0);

                // ── Call 4 — workflow file contents (non-fatal) ──────────────
                JsonNode workflowArray = workflows.path("workflows");
                int limit = Math.min(count, 5);
                for (int i = 0; i < limit; i++) {
                    String path = workflowArray.get(i).path("path").asText();
                    try {
                        JsonNode fileNode = webClient.get()
                                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .block();
                        if (fileNode != null) {
                            String encoded = fileNode.path("content").asText();
                            String decoded = new String(java.util.Base64.getMimeDecoder().decode(encoded));
                            workflowContents.add(decoded);
                        }
                    } catch (Exception e) { /* skip failed workflow file */ }
                }
            }
        } catch (Exception e) { /* no workflows */ }
        data.setWorkflowContents(workflowContents);

        // ── Call 5 — releases (non-fatal) ────────────────────────────────────
        try {
            JsonNode releases = webClient.get()
                    .uri("/repos/{owner}/{repo}/releases?per_page=10", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (releases != null && releases.isArray() && releases.size() > 0) {
                data.setTotalReleases(releases.size());
                data.setLatestReleaseDate(releases.get(0).path("published_at").asText());
                if (releases.size() > 1) {
                    long firstMs = java.time.Instant.parse(releases.get(0).path("published_at").asText()).toEpochMilli();
                    long lastMs  = java.time.Instant.parse(releases.get(releases.size() - 1).path("published_at").asText()).toEpochMilli();
                    double totalDays = (firstMs - lastMs) / (1000.0 * 60 * 60 * 24);
                    data.setAvgDaysBetweenReleases(totalDays / (releases.size() - 1));
                }
            }
        } catch (Exception e) { /* no releases */ }

        // ── Call 6 — languages (non-fatal) ───────────────────────────────────
        try {
            JsonNode languagesNode = webClient.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            Map<String, Long> languages = new java.util.LinkedHashMap<>();
            if (languagesNode != null) {
                languagesNode.fields().forEachRemaining(entry ->
                    languages.put(entry.getKey(), entry.getValue().asLong()));
            }
            data.setLanguages(languages);
        } catch (Exception e) { /* language data unavailable */ }

        // ── Call 7 — README (non-fatal) ──────────────────────────────────────
        try {
            JsonNode readmeNode = webClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (readmeNode != null) {
                String encoded = readmeNode.path("content").asText();
                String decoded = new String(java.util.Base64.getMimeDecoder().decode(encoded));
                data.setReadmeContent(decoded.length() > 3000 ? decoded.substring(0, 3000) + "\n..." : decoded);
            }
        } catch (Exception e) { /* no readme */ }

        // ── Call 8 — contributors (non-fatal) ────────────────────────────────
        try {
            JsonNode contribNode = webClient.get()
                    .uri("/repos/{owner}/{repo}/contributors?per_page=5", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            List<String> contributors = new ArrayList<>();
            if (contribNode != null && contribNode.isArray()) {
                contribNode.forEach(c -> contributors.add(
                    c.path("login").asText() + "|" +
                    c.path("contributions").asInt() + "|" +
                    c.path("avatar_url").asText()
                ));
            }
            data.setContributors(contributors);
        } catch (Exception e) { /* contributor data unavailable */ }

        return data;
    }
}