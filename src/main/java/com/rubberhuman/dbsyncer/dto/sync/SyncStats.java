package com.rubberhuman.dbsyncer.dto.sync;

import lombok.Data;

@Data
public class SyncStats {
    private Long pendingCount; // 待处理 (status=0)
    private Long successCount; // 成功 (status=1)
    private Long failedCount;  // 失败 (status=2)
    private Long totalCount;   // 总数
}
