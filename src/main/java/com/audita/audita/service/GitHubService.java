package com.audita.audita.service;

import com.audita.audita.model.RepoData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

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

        // Call 1 — basic repo info
        JsonNode repoInfo = webClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

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

        // Call 2 — root file listing
        JsonNode contents = webClient.get()
                .uri("/repos/{owner}/{repo}/contents", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<String> rootFiles = new ArrayList<>();
        if (contents != null && contents.isArray()) {
            contents.forEach(file -> rootFiles.add(file.path("name").asText()));
        }
        data.setRootFiles(rootFiles);

        // Call 3 — workflows list
        JsonNode workflows = webClient.get()
                .uri("/repos/{owner}/{repo}/actions/workflows", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<String> workflowContents = new ArrayList<>();
        if (workflows != null) {
            int count = workflows.path("total_count").asInt();
            data.setWorkflowCount(count);
            data.setHasWorkflows(count > 0);

            // Call 4 — fetch content of each workflow file (max 5)
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
                        String decoded = new String(
                            java.util.Base64.getMimeDecoder().decode(encoded)
                        );
                        workflowContents.add(decoded);
                    }
                } catch (Exception e) {
                    // skip if one workflow fails to fetch
                }
            }
        }
        data.setWorkflowContents(workflowContents);

        // Call 5 — releases
        try {
            JsonNode releases = webClient.get()
                    .uri("/repos/{owner}/{repo}/releases?per_page=10", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (releases != null && releases.isArray() && releases.size() > 0) {
                data.setTotalReleases(releases.size());
                data.setLatestReleaseDate(releases.get(0).path("published_at").asText());

                // Calculate average days between releases
                if (releases.size() > 1) {
                    long firstMs = java.time.Instant.parse(
                        releases.get(0).path("published_at").asText()).toEpochMilli();
                    long lastMs = java.time.Instant.parse(
                        releases.get(releases.size() - 1).path("published_at").asText()).toEpochMilli();
                    double totalDays = (firstMs - lastMs) / (1000.0 * 60 * 60 * 24);
                    data.setAvgDaysBetweenReleases(totalDays / (releases.size() - 1));
                }
            }
        } catch (Exception e) {
            // repo may have no releases
        }

        return data;
    }
}