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

        // Topics
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

        // Call 3 — workflows
        JsonNode workflows = webClient.get()
                .uri("/repos/{owner}/{repo}/actions/workflows", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (workflows != null) {
            int count = workflows.path("total_count").asInt();
            data.setWorkflowCount(count);
            data.setHasWorkflows(count > 0);
        }

        return data;
    }
}