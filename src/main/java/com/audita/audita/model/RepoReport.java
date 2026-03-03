package com.audita.audita.model;

import lombok.Data;
import java.util.List;

@Data
public class RepoReport {
    private String name;
    private String fullName;
    private String description;
    private int stars;
    private int forks;
    private int openIssues;
    private String lastPushed;
    private String license;
    private List<String> topics;
    private int totalScore;
    private String grade;
    private List<CategoryScore> categories;
    private List<String> suggestions;
}