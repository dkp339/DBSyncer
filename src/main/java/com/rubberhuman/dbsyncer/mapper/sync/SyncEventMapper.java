package com.rubberhuman.dbsyncer.mapper.sync;

import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import com.rubberhuman.dbsyncer.util.DynamicDbUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class SyncEventMapper {

    @Autowired
    private DynamicDbUtil dynamicDbUtil;

    // 手动定义映射规则：把 ResultSet 转为 Entity
    @Getter
    private final RowMapper<SyncEvent> rowMapper = (rs, rowNum) -> {
        SyncEvent event = new SyncEvent();
        event.setId(rs.getLong("id"));
        event.setTableName(rs.getString("table_name"));
        event.setOpType(rs.getString("op_type"));
        event.setPkColumnName(rs.getString("pk_column_name"));
        event.setPkValue(rs.getString("pk_value"));
        event.setStatus(rs.getInt("status"));
        event.setOpTime(rs.getTimestamp("op_time"));
        event.setSourceDbType(DatabaseType.fromCode(rs.getString("source_db_type")));
        event.setErrorMsg(rs.getString("error_msg"));
        event.setDataVersion(rs.getLong("data_version"));
        return event;
    };

    // 查询未处理的同步事件
    public List<SyncEvent> selectUnprocessed(Long sourceId, int batchSize) {
        JdbcTemplate jdbcTemplate = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);

        String sql = "SELECT * FROM sync_event WHERE status = 0 ORDER BY id ASC";

        jdbcTemplate.setMaxRows(batchSize);
        try {
            return jdbcTemplate.query(sql, rowMapper);
        } finally {
            jdbcTemplate.setMaxRows(-1); // 还原设置
        }
    }

    public int updateStatusSuccess(Long sourceId, Long eventId) {
        JdbcTemplate jdbcTemplate = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);
        String sql = "UPDATE sync_event SET status = 1, error_msg = NULL WHERE id = ?";
        return jdbcTemplate.update(sql, eventId);
    }

    public int updateStatusFail(Long sourceId, Long eventId, String errorMsg) {
        JdbcTemplate jdbcTemplate = dynamicDbUtil.getSyncWorkerJdbcTemplate(sourceId);
        String sql = "UPDATE sync_event SET status = 2, error_msg = ? WHERE id = ?";
        return jdbcTemplate.update(sql, errorMsg, eventId);
    }

    // 查询所有事件，供前端管理界面查看日志使用
    public List<SyncEvent> selectAll(Long sourceId, int limit) {
        JdbcTemplate jdbcTemplate = dynamicDbUtil.getJdbcTemplate(sourceId);
        String sql = "SELECT * FROM sync_event ORDER BY id DESC";
        jdbcTemplate.setMaxRows(limit);
        try {
            return jdbcTemplate.query(sql, rowMapper);
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            jdbcTemplate.setMaxRows(-1);
        }
    }
}
