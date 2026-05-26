package com.jinshu.common.entity;

import java.util.List;

public class PdfRenderConfig {

    private Long reportId;
    private String colorSpace;
    private boolean watermarkEnabled;
    private Integer pageCount;
    private List<Segment> segments;
    private String outputPath;
    private boolean fallback;
    private String renderUrl;

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getColorSpace() { return colorSpace; }
    public void setColorSpace(String colorSpace) { this.colorSpace = colorSpace; }
    public boolean isWatermarkEnabled() { return watermarkEnabled; }
    public void setWatermarkEnabled(boolean watermarkEnabled) { this.watermarkEnabled = watermarkEnabled; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public List<Segment> getSegments() { return segments; }
    public void setSegments(List<Segment> segments) { this.segments = segments; }
    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }
    public String getRenderUrl() { return renderUrl; }
    public void setRenderUrl(String renderUrl) { this.renderUrl = renderUrl; }

    public static class Segment {
        private int seq;
        private int pageFrom;
        private int pageTo;
        private String status;
        private String outputPath;
        private String md5;

        public int getSeq() { return seq; }
        public void setSeq(int seq) { this.seq = seq; }
        public int getPageFrom() { return pageFrom; }
        public void setPageFrom(int pageFrom) { this.pageFrom = pageFrom; }
        public int getPageTo() { return pageTo; }
        public void setPageTo(int pageTo) { this.pageTo = pageTo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }
    }
}
