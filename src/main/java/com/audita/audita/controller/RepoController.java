package com.audita.audita.controller;

import com.audita.audita.model.RepoData;
import com.audita.audita.model.RepoReport;
import com.audita.audita.service.GitHubService;
import com.audita.audita.service.ScoringService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RepoController {

    private final GitHubService gitHubService;
    private final ScoringService scoringService;

    public RepoController(GitHubService gitHubService, ScoringService scoringService) {
        this.gitHubService = gitHubService;
        this.scoringService = scoringService;
    }

    @GetMapping("/analyze")
    public RepoReport analyze(@RequestParam String repoUrl) {
    String[] parts = repoUrl
            .replace("https://github.com/", "")
            .split("/");

    String owner = parts[0];
    String repo = parts[1];
    // Strip any extra path like /tree/branch or /blob/main etc
    repo = repo.split("\\?")[0]; // remove query params too

        RepoData data = gitHubService.fetchRepoData(owner, repo);
        return scoringService.score(data);
    }
}