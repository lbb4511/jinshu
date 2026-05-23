package com.jinshu.common.enums;

public enum ReportStatus {
    DRAFT("草稿"),
    PENDING_REVIEW("待审查"),
    APPROVED("已通过"),
    REJECTED("已驳回"),
    PUBLISHED("已公开");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isValidTransition(String from, String to) {
        if (from == null) {
            return DRAFT.name().equals(to);
        }
        switch (from) {
            case "DRAFT":
                return "PENDING_REVIEW".equals(to) || "DELETED".equals(to);
            case "PENDING_REVIEW":
                return "APPROVED".equals(to) || "REJECTED".equals(to);
            case "REJECTED":
                return "DRAFT".equals(to) || "PENDING_REVIEW".equals(to);
            case "APPROVED":
                return "PUBLISHED".equals(to) || "REJECTED".equals(to);
            case "PUBLISHED":
                return "REJECTED".equals(to);
            default:
                return false;
        }
    }
}
