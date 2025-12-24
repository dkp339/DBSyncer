package com.rubberhuman.dbsyncer.service.sync;

import com.rubberhuman.dbsyncer.dto.sync.SyncStats;
import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;

import java.util.List;

public interface SyncAdminService {
    SyncStats getStats(Long sourceId);

    List<SyncEvent> listLogs(Long sourceId, Integer status, int page, int size);

    void retryEvent(Long sourceId, Long eventId);

    void skipEvent(Long sourceId, Long eventId);
}
