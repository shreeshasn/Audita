package com.audita.audita.model;

import lombok.Data;
import java.util.List;

@Data
public class CategoryScore {
    private String name;
    private int score;
    private int maxScore;
    private List<CheckItem> checks;

    @Data
    public static class CheckItem {
        private String label;
        private String detail;
        private String status; // "pass", "fail", "warn"
        private int points;

        public CheckItem(String label, String detail, String status, int points) {
            this.label = label;
            this.detail = detail;
            this.status = status;
            this.points = points;
        }
    }
}