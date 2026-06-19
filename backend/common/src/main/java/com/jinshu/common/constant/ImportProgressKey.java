package com.jinshu.common.constant;

/**
 * 导入任务 Redis 进度 Key 与字段定义。
 *
 * <p>进度存储在同一个 Hash 中，按分片记录进度，避免多分片并发时互相覆盖。
 */
public final class ImportProgressKey {

    private ImportProgressKey() {
    }

    public static final String PREFIX = "jinshu:import:progress:";

    public static final String TOTAL_SHARDS = "totalShards";

    public static final String TOTAL_ROWS = "totalRows";

    public static String key(Long taskId) {
        return PREFIX + taskId;
    }

    public static String processedRows(int shardSeq) {
        return "processedRows:" + shardSeq;
    }

    public static String failedRows(int shardSeq) {
        return "failedRows:" + shardSeq;
    }

    public static String status(int shardSeq) {
        return "status:" + shardSeq;
    }
}
