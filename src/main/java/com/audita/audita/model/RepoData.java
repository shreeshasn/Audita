package com.audita.audita.model;

import lombok.Data;
import java.util.List;

@Data
public class RepoData {
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
    private boolean hasWorkflows;
    private int workflowCount;
}