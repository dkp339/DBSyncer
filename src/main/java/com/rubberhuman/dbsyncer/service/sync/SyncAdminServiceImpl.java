package com.rubberhuman.dbsyncer.service.sync;

import com.rubberhuman.dbsyncer.dto.sync.SyncStats;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import com.rubberhuman.dbsyncer.exception.BusinessException;
import com.rubberhuman.dbsyncer.mapper.sync.SyncEventMapper;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import com.rubberhuman.dbsyncer.util.DynamicDbUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SyncAdminServiceImpl implements SyncAdminService {

    @Autowired
    private DynamicDbUtil dynamicDbUtil;

    @Autowired
    private SyncEventMapper eventMapper;

    @Autowired
    private DataSourceConfigService configService;

    // 1. 获取统计概览
    public SyncStats getStats(Long sourceId) {
        JdbcTemplate jt = dynamicDbUtil.getJdbcTemplate(sourceId);

        // 使用 SUM(CASE...) 一次性查出所有状态的数量
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) as pending, " +
                "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) as success, " +
                "COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) as failed, " +
                "COUNT(*) as total " +
                "FROM sync_event";

        Map<String, Object> map = jt.queryForMap(sql);

        SyncStats dto = new SyncStats();
        dto.setPendingCount(((Number) map.get("pending")).longValue());
        dto.setSuccessCount(((Number) map.get("success")).longValue());
        dto.setFailedCount(((Number) map.get("failed")).longValue());
        dto.setTotalCount(((Number) map.get("total")).longValue());

        return dto;
    }

    // 2. 分页查询日志
    public List<SyncEvent> listLogs(Long sourceId, Integer status, int page, int size) {
        JdbcTemplate jt = dynamicDbUtil.getJdbcTemplate(sourceId);

        // 1. 获取当前源库的类型，以便决定用哪种分页语法
        DataSourceConfig config = configService.getById(sourceId);
        if (config == null) {
            throw new BusinessException("数据源不存在");
        }

        // 2. 构建基础 SQL
        StringBuilder baseSql = new StringBuilder("SELECT * FROM sync_event WHERE 1=1 ");
        if (status != null) {
            baseSql.append("AND status = ").append(status).append(" ");
        }
        baseSql.append("ORDER BY id DESC "); // 必须要有排序，否则 SQLServer 分页会报错

        // 3. 计算偏移量
        int offset = (page - 1) * size;

        // 4. 根据数据库类型拼接分页语句
        String sql = buildPaginationSql(config.getDbType(), baseSql.toString(), offset, size);

        return jt.query(sql, eventMapper.getRowMapper());
    }

    // 3. 重试事件 (人工修复)
    // 逻辑：把状态改回 0 (未处理)，清空错误信息。SyncEngine 下一轮轮询时会自动抓取它。
    public void retryEvent(Long sourceId, Long eventId) {
        JdbcTemplate jt = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId); // 使用 Worker 账号或普通账号均可

        // 先检查是否存在
        String checkSql = "SELECT count(*) FROM sync_event WHERE id = ?";
        Long count = jt.queryForObject(checkSql, Long.class, eventId);
        if (count == null || count == 0) {
            throw new BusinessException("未找到 ID 为 " + eventId + " 的同步事件");
        }

        String sql = "UPDATE sync_event SET status = 0, error_msg = NULL WHERE id = ?";
        jt.update(sql, eventId);
    }

    // 4. 强制跳过 (人工忽略)
    // 逻辑：把状态改为 1 (成功)，并备注是管理员操作。
    public void skipEvent(Long sourceId, Long eventId) {
        JdbcTemplate jt = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);

        String sql = "UPDATE sync_event SET status = 1, error_msg = '[管理员强制标记成功]' WHERE id = ?";
        int rows = jt.update(sql, eventId);

        if (rows == 0) {
            throw new BusinessException("操作失败，事件可能不存在");
        }
    }

    // 辅助方法：生成不同数据库的分页 SQL
    private String buildPaginationSql(DatabaseType dbType, String baseSql, int offset, int limit) {
        switch (dbType) {
            case MYSQL:
            case POSTGRESQL:
                // MySQL & PG 语法: LIMIT limit OFFSET offset
                return baseSql + " LIMIT " + limit + " OFFSET " + offset;

            case ORACLE:
            case SQL_SERVER:
                // Oracle 12c+ 和 SQL Server 2012+ 标准语法:
                // OFFSET offset ROWS FETCH NEXT limit ROWS ONLY
                return baseSql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";

            default:
                // 默认尝试 MySQL 语法，或者抛出不支持
                return baseSql + " LIMIT " + limit + " OFFSET " + offset;
        }
    }
}
