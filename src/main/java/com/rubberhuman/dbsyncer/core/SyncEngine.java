package com.rubberhuman.dbsyncer.core;

import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.exception.BusinessException;
import com.rubberhuman.dbsyncer.exception.SyncConflictException;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import com.rubberhuman.dbsyncer.service.sync.SyncAlertService;
import com.rubberhuman.dbsyncer.service.sync.SyncEventService;
import com.rubberhuman.dbsyncer.util.DynamicDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SyncEngine {

    @Autowired
    private SyncEventService syncEventService;

    @Autowired
    private DataSourceConfigService configService;

    @Autowired
    private SyncAlertService alertService;

    @Autowired
    private DynamicDbUtil dynamicDbUtil;

    @Value("${dbsyncer.sync-worker.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${dbsyncer.sync-worker.poll-interval-ms}")
    public void startSync() {

        log.info("=== 数据同步周期开始 ===");

        List<DataSourceConfig> activeSources = configService.listEnabled();

        if (activeSources == null || activeSources.isEmpty()) {
            log.info("当前未发现已启用的数据源，本轮同步结束");
            return;
        }

        log.info("发现 {} 个已启用的数据源，开始依次处理", activeSources.size());

        for (DataSourceConfig sourceDb : activeSources) {
            try {
                log.info("开始处理源数据库：{}", sourceDb.getSourceName());
                processSource(sourceDb, activeSources);
                log.info("源数据库处理完成：{}", sourceDb.getSourceName());
            } catch (Exception e) {
                log.error("处理源数据库失败：{}，已跳过，继续下一个", sourceDb.getSourceName(), e);
            }
        }

        log.info("=== 数据同步周期结束 ===");
    }

    // ------ 单源同步 ------
    // 处理单个数据源的 sync_event
    private void processSource(DataSourceConfig sourceDb, List<DataSourceConfig> allDbs) {
        Long sourceId = sourceDb.getSourceId();

        // debug
        if ("POSTGRESQL".equals(sourceDb.getDbType().name())) { // 只针对 PG 调试
            try {
                JdbcTemplate jt = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);

                // 1. 查当前连接的数据库名
                String currentDb = jt.queryForObject("SELECT current_database()", String.class);

                // 2. 查当前的 Schema 搜索路径
                String searchPath = jt.queryForObject("SHOW search_path", String.class);

                // 3. 查当前 Schema
                String currentSchema = jt.queryForObject("SELECT current_schema()", String.class);

                log.info(">>> [Debug PG] Source={} | DB={} | Schema={} | SearchPath={}",
                        sourceDb.getSourceName(), currentDb, currentSchema, searchPath);

            } catch (Exception e) {
                log.error(">>> [Debug PG] 探针执行失败", e);
            }
        }

        // 拉取所有 sourceDb 中未同步的事件
        List<SyncEvent> events = syncEventService.getUnprocessedList(sourceId, batchSize);
        if (events.isEmpty()) {
            return;
        }
        log.info("在数据源「{}」中发现 {} 条待同步事件", sourceDb.getSourceName(), events.size());

        // 计算需要同步的所有数据库
        List<DataSourceConfig> targetDbs = allDbs.stream()
                .filter(db -> !db.getSourceId().equals(sourceId))
                .toList();

        for (SyncEvent event : events) {
            processEvent(sourceDb, targetDbs, event);
        }
    }

    private void processEvent(DataSourceConfig sourceDb, List<DataSourceConfig> targetDbs, SyncEvent event) {
        try {
            // 合法性校验
            if (!isValidEvent(event)) {
                syncEventService.markFail(sourceDb.getSourceId(), event.getId(), "无效的同步事件数据");
                log.warn("【同步引擎】事件 {} 校验失败，已标记为失败", event.getId());
                return;
            }

            // 从源数据库获取需要同步的数据
            // INSERT/UPDATE 任务需要回查数据，DELETE 直接修改
            Map<String, Object> sourceData = null;
            if ("INSERT".equals(event.getOpType()) || "UPDATE".equals(event.getOpType())) {
                sourceData = fetchSourceData(sourceDb.getSourceId(), event);
                if (sourceData == null) {
                    syncEventService.markFail(sourceDb.getSourceId(), event.getId(), "源数据不存在，可能已被删除");
                    log.warn("【同步引擎】事件 {} 回查源数据失败，数据不存在", event.getId());
                    return;
                }
            }

            // 同步任务调度
            boolean allSuccess = true;
            StringBuilder errorLog = new StringBuilder();

            for (DataSourceConfig targetDb : targetDbs) {
                try {
                    syncToTarget(targetDb, event, sourceData);
                    log.info("【同步引擎】事件 {} 已成功同步到目标库「{}」", event.getId(), targetDb.getSourceName());
                } catch (SyncConflictException ce) {
                    allSuccess = false;

                    errorLog.append("【冲突】").append(targetDb.getSourceName()).append("：").append(ce.getMessage()).append("；");
                    log.error("检测到数据同步冲突，事件ID={}", event.getId(), ce);

                    alertService.sendConflictAlert(sourceDb.getSourceName(), targetDb.getSourceName(), ce.getTable(), ce.getPk(), ce.getMessage());
                } catch (Exception e) {
                    allSuccess = false;
                    errorLog.append("「").append(targetDb.getSourceName()).append("」同步失败：").append(e.getMessage()).append("；");
                    log.error("【同步引擎】事件 {} 同步到目标库「{}」失败", event.getId(), targetDb.getSourceName(), e);
                }
            }

            if (allSuccess) {
                syncEventService.markSuccess(sourceDb.getSourceId(), event.getId());
            } else {
                syncEventService.markFail(sourceDb.getSourceId(), event.getId(), errorLog.toString());
            }
        } catch (Exception e) {
            syncEventService.markFail(sourceDb.getSourceId(), event.getId(), "系统异常：" + e.getMessage());
            log.error("【同步引擎】处理事件 {} 时发生系统异常", event.getId(), e);
        }
    }

    private void syncToTarget(DataSourceConfig targetDb, SyncEvent event, Map<String, Object> data) {
        JdbcTemplate targetJt = dynamicDbUtil.getSyncWorkerJdbcTemplate(targetDb.getSourceId());
        String tableName = event.getTableName();
        String pkCol = event.getPkColumnName();
        String pkVal = event.getPkValue();
        String opType = event.getOpType();

        // 只有 UPDATE 操作检测冲突
        if ("UPDATE".equals(opType)) {
            checkConflict(targetJt, tableName, pkCol, pkVal, event.getDataVersion());
        }

        switch (opType) {
            case "INSERT", "UPDATE":
                executeUpsert(targetJt, tableName, pkCol, pkVal, data);
                break;
            case "DELETE":
                executeDelete(targetJt, tableName, pkCol, pkVal);
                break;
            default:
                throw new BusinessException("未知的同步操作类型：" + event.getOpType());
        }
    }

    // 乐观锁冲突检测
    private void checkConflict(JdbcTemplate jt, String table, String pkCol, String pkVal, Long eventVersion) {
        if (eventVersion == null) {
            throw new BusinessException("同步事件缺少数据版本号，无法进行冲突检测，" + "表=" + table + ", 主键=" + pkVal);
        }

        try {
            String sql = "SELECT sync_version FROM " + table + " WHERE " + pkCol + " = ?";

            Long targetVersion = jt.queryForObject(sql, Long.class, pkVal);

            if (targetVersion != null && targetVersion > eventVersion) {
                throw new SyncConflictException("目标库版本高于事件版本", table, pkVal, eventVersion, targetVersion);
            }

        } catch (EmptyResultDataAccessException e) {
            // 目标库查不到数据，说明是新数据，无冲突
        }
    }


    // ------ 辅助方法 ------
    private boolean isValidEvent(SyncEvent event) {
        return event.getTableName() != null
                && event.getPkColumnName() != null
                && event.getPkValue() != null
                && event.getOpType() != null;
    }

    private Map<String, Object> fetchSourceData(Long sourceId, SyncEvent event) {
        JdbcTemplate jt = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);
        String sql = "SELECT * FROM " + event.getTableName() + " WHERE " + event.getPkColumnName() + " = ?";
        try {
            return jt.queryForMap(sql, event.getPkValue());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void executeUpsert(JdbcTemplate jt, String tableName, String pkCol, String pkVal, Map<String, Object> data) {
        try {
            int rows = executeUpdate(jt, tableName, pkCol, pkVal, data);
            if (rows == 0) {
                try {
                    executeInsert(jt, tableName, data);
                } catch (Exception e) {
                    log.warn("INSERT 失败，尝试转为 UPDATE : {}", e.getMessage());
                    executeUpdate(jt, tableName, pkCol, pkVal, data);
                }
            }
        } catch (Exception e) {
            log.error("同步到目标库失败。表: {}, 主键: {}, 错误: {}", tableName, pkVal, e.getCause().getMessage());
            throw e;
        }
    }

    private void executeInsert(JdbcTemplate jt, String tableName, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> args = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(entry.getKey());
            values.append("?");
            args.add(entry.getValue());
            i++;
        }

        sql.append(") ").append(values).append(")");
        jt.update(sql.toString(), args.toArray());
    }

    private int executeUpdate(JdbcTemplate jt, String tableName, String pkCol, String pkVal, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> args = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            // 主键不参与 SET 操作
            if (entry.getKey().equalsIgnoreCase(pkCol)) {
                continue;
            }
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append(" = ?");
            args.add(entry.getValue());
            i++;
        }

        // 如果没有可更新的列，直接返回 1 (视作成功)
        if (args.isEmpty()) {
            return 1;
        }

        sql.append(" WHERE ").append(pkCol).append(" = ?");
        args.add(pkVal);

        return jt.update(sql.toString(), args.toArray());
    }

    private void executeDelete(JdbcTemplate jt, String tableName, String pkCol, String pkVal) {
        String sql = "DELETE FROM " + tableName + " WHERE " + pkCol + " = ?";
        jt.update(sql, pkVal);
    }
}
