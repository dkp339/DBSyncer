package com.rubberhuman.dbsyncer.service.sync;

import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;

import java.util.List;

public interface SyncEventService {
    List<SyncEvent> getUnprocessedList(Long sourceId, int batchSize);

    void markSuccess(Long sourceId, Long eventId);

    void markFail(Long sourceId, Long eventId, String msg);

    List<SyncEvent> getRecentLogs(Long sourceId);

    int resetFailedEventsInPast24Hours(Long sourceId);
}
