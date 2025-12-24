package com.rubberhuman.dbsyncer.service.sync;

import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.mapper.sync.SyncEventMapper;
import com.rubberhuman.dbsyncer.util.DynamicDbUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncEventServiceImpl implements SyncEventService {

    @Autowired
    private SyncEventMapper syncEventMapper;

    @Autowired
    private DynamicDbUtil dynamicDbUtil;

    // Task: 获取未处理事件
    public List<SyncEvent> getUnprocessedList(Long sourceId, int batchSize) {
        return syncEventMapper.selectUnprocessed(sourceId, batchSize);
    }

    // Task: 标记成功
    public void markSuccess(Long sourceId, Long eventId) {
        syncEventMapper.updateStatusSuccess(sourceId, eventId);
    }

    // Task: 标记失败
    public void markFail(Long sourceId, Long eventId, String msg) {
        // 防止太长存不进去
        if (msg != null && msg.length() > 500) {
            msg = msg.substring(0, 500) + "...";
        }
        syncEventMapper.updateStatusFail(sourceId, eventId, msg);
    }

    // Controller: 获取最近的同步日志
    public List<SyncEvent> getRecentLogs(Long sourceId) {
        return syncEventMapper.selectAll(sourceId, 50);
    }


    // 重置过去 24 小时内的失败任务
    public int resetFailedEventsInPast24Hours(Long sourceId) {
        JdbcTemplate jt = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);

        String sql = "UPDATE sync_event SET status = 0, error_msg = NULL " +
                "WHERE status = 2 AND op_time > DATE_SUB(NOW(), INTERVAL 1 DAY)";

        return jt.update(sql);
    }
}
