package com.audita.audita.model;

import lombok.Data;
import java.util.List;

@Data
public class RepoData {
    // Basic info
    private String name;
    private String fullName;
    private String description;
    private int stars;
    private int forks;
    private int openIssues;
    private String lastPushed;
    private String license;
    private List<String> topics;
    private List<String> rootFiles;

    // Workflows
    private boolean hasWorkflows;
    private int workflowCount;
    private List<String> workflowContents;

    // Releases
    private int totalReleases;
    private String latestReleaseDate;
    private double avgDaysBetweenReleases;
}