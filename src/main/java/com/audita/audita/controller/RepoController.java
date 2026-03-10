package com.audita.audita.controller;

import com.audita.audita.model.RepoData;
import com.audita.audita.model.RepoReport;
import com.audita.audita.service.GitHubService;
import com.audita.audita.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> analyze(@RequestParam String repoUrl) {
        try {
            // Validate URL before touching GitHub
            if (repoUrl == null || !repoUrl.contains("github.com/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please provide a full GitHub URL (e.g. https://github.com/owner/repo)."));
            }

            String[] parts = repoUrl
                    .replace("https://github.com/", "")
                    .replace("http://github.com/", "")
                    .split("/");

            if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Could not read owner/repo from that URL. Expected format: github.com/owner/repo"));
            }

            String owner = parts[0];
            String repo  = parts[1].split("\\?")[0]; // strip query params

            RepoData data = gitHubService.fetchRepoData(owner, repo);
            return ResponseEntity.ok(scoringService.score(data));

        } catch (RuntimeException e) {
            // Known, user-facing error thrown by GitHubService
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Unexpected — don't leak stack trace
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Something went wrong on our end. Please try again."));
        }
    }
}